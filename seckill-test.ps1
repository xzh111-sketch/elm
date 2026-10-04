<#
==========================================================================
 饿了么项目 · 秒杀防超卖压测脚本
==========================================================================
 用途：验证秒杀功能「不超卖、不重复购买」

 两种模式：
   -Mode stock   多个不同用户同时抢（默认）—— 验证不超卖
                期望：成功人数 = 库存数，数据库库存精确归零，不会变负数
   -Mode repeat  同一个用户并发点 N 次 —— 验证防重复购买
                期望：只有 1 次成功，其余被「每人限购 1 件」拦下

 用法示例：
   # 默认：30 个用户抢活动 3（库存 3）
   .\seckill-test.ps1

   # 指定后端地址、活动、并发人数
   .\seckill-test.ps1 -BaseUrl http://localhost:8080 -SeckillId 1 -Users 50

   # 指定库存（脚本会把数据库库存重置成这个值，方便重复压测）
   .\seckill-test.ps1 -SeckillId 1 -Users 100 -Stock 10

   # 测「同一用户连点」
   .\seckill-test.ps1 -Mode repeat -SeckillId 1 -Users 10

 前置条件：
   1. 后端已启动，Redis 和 RabbitMQ 已启动
   2. 需要 PowerShell 7+（.NET 的 HttpClient 才能真并发）
   3. 脚本会自动注册测试账号（sk01、sk02...）并给每个账号建收货地址，
      因为秒杀下单需要收货地址；如果账号已存在会直接复用
   4. 每次运行前会清理 sk 前缀测试账号的秒杀记录/订单，并把库存重置为 -Stock 指定的值，
      所以脚本可以反复运行；真实用户的数据不会被碰
==========================================================================
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$SeckillId = 3,
    [int]$Users = 30,
    [string]$UserPrefix = "sk",
    [string]$Password = "123456",
    [ValidateSet("stock", "repeat")]
    [string]$Mode = "stock",
    # 开抢前把数据库库存重置为这个值（脚本可重复运行的关键）
    [int]$Stock = 10,
    # 数据库校验（脚本会自动找 mysql.exe，找不到就跳过）
    [string]$MysqlUser = "root",
    # 不写默认密码：请用 -MysqlPassword 传入，或先设置 MYSQL_PWD 环境变量
    [string]$MysqlPassword = $env:MYSQL_PWD,
    [string]$MysqlDb = "elm",
    [switch]$SkipDbCheck
)

$ErrorActionPreference = "Stop"
$ContentType = "application/x-www-form-urlencoded"

function Write-Step([string]$text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Cyan
}

function Invoke-Login([string]$userId) {
    $resp = Invoke-RestMethod -Uri "$BaseUrl/UserController/login" -Method POST `
        -Body "userId=$userId&password=$Password" -ContentType $ContentType
    if (-not $resp.success) { return $null }
    return $resp.token
}

# 没有测试账号就自动注册一个，并给它建一条收货地址
function Initialize-TestUser([string]$userId, [int]$index) {
    $token = Invoke-Login $userId
    if ($token) { return $token }

    Invoke-RestMethod -Uri "$BaseUrl/UserController/saveUser" -Method POST `
        -Body "userId=$userId&password=$Password&userName=seckill$index&userSex=1" `
        -ContentType $ContentType | Out-Null

    $token = Invoke-Login $userId
    if (-not $token) { throw "注册后仍然登录失败：$userId" }

    $headers = @{ Authorization = "Bearer $token" }
    Invoke-RestMethod -Uri "$BaseUrl/DeliveryAddressController/saveDeliveryAddress" -Method POST `
        -Headers $headers -ContentType $ContentType `
        -Body "contactName=seckill$index&contactSex=1&contactTel=13800000000&address=压测地址$index" | Out-Null
    return $token
}

function Get-SeckillList([hashtable]$headers) {
    return Invoke-RestMethod -Uri "$BaseUrl/api/seckill/list" -Headers $headers
}

# ---- 数据库查询（可选，找不到 mysql.exe 就返回 null） ----
$script:mysqlExe = $null
function Get-MysqlExe {
    if (-not $script:mysqlExe) {
        $script:mysqlExe = @(
            "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
            "D:\MySQL\MySQL Server 8.0\bin\mysql.exe"
        ) | Where-Object { Test-Path $_ } | Select-Object -First 1
    }
    return $script:mysqlExe
}

function Invoke-SqlScalar([string]$sql) {
    $mysql = Get-MysqlExe
    if (-not $mysql) { return $null }
    $env:MYSQL_PWD = $MysqlPassword
    $out = & $mysql -h 127.0.0.1 -u $MysqlUser -D $MysqlDb -N -B -e $sql 2>$null | Select-Object -First 1
    if ($null -eq $out -or "$out" -eq "") { return $null }
    return [int]$out
}

# 真并发：所有请求同时发出，不用排队
function Invoke-ConcurrentSeckill([string[]]$tokens, [string]$url) {
    $handler = [System.Net.Http.SocketsHttpHandler]::new()
    $handler.MaxConnectionsPerServer = 500
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(60)

    $tasks = [System.Collections.Generic.List[System.Threading.Tasks.Task]]::new()
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    foreach ($token in $tokens) {
        $req = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, $url)
        $req.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $token)
        $req.Content = [System.Net.Http.StringContent]::new("")
        $tasks.Add($client.SendAsync($req))
    }
    [System.Threading.Tasks.Task]::WaitAll($tasks.ToArray())
    $sw.Stop()

    $bodies = @($tasks | ForEach-Object { $_.Result.Content.ReadAsStringAsync().Result })
    $client.Dispose()
    return @{ ElapsedMs = $sw.ElapsedMilliseconds; Bodies = $bodies }
}

function Test-BaseUrl {
    try {
        Invoke-RestMethod -Uri "$BaseUrl/UserController/getUserById?userId=1" -TimeoutSec 5 | Out-Null
        return $true
    } catch {
        if ($_.Exception.Response) { return $true }
        return $false
    }
}

# ---------------------------------------------------------------- 开始
Write-Host ""
Write-Host "秒杀防超卖压测  |  后端=$BaseUrl  活动=$SeckillId  并发=$Users  模式=$Mode" -ForegroundColor Yellow

if ($PSVersionTable.PSVersion.Major -lt 7) {
    Write-Host "警告：建议用 PowerShell 7+ 运行，低版本 .NET 的连接数限制会导致并发不真实" -ForegroundColor Red
}
if (-not (Test-BaseUrl)) {
    Write-Host "连不上后端 $BaseUrl，请先启动 Spring Boot" -ForegroundColor Red
    exit 1
}

# 1. 准备测试账号
Write-Step "1. 准备 $Users 个测试账号（$UserPrefix 01 ~ $UserPrefix $Users）"
$tokens = @()
for ($i = 1; $i -le $Users; $i++) {
    $userId = "{0}{1:D2}" -f $UserPrefix, $i
    $tokens += Initialize-TestUser $userId $i
    if ($i % 10 -eq 0) { Write-Host "  已就绪 $i/$Users" }
}
Write-Host "  ✅ $($tokens.Count) 个账号全部可登录" -ForegroundColor Green

# 2. 重置测试数据：清理测试账号的历史记录 + 库存重置 + 同步到 Redis
Write-Step "2. 重置测试数据（只清理 $UserPrefix 前缀的测试账号，不动真实用户）"
$headers0 = @{ Authorization = "Bearer $($tokens[0])" }
if (Get-MysqlExe) {
    Invoke-SqlScalar "delete from seckill_order where userId like '$UserPrefix%';" | Out-Null
    Invoke-SqlScalar "delete from orders where userId like '$UserPrefix%';" | Out-Null
    Invoke-SqlScalar "update seckill set seckillStock = $Stock where seckillId = $SeckillId;" | Out-Null
    Write-Host "  ✅ 已清理测试账号的历史秒杀记录/订单，库存重置为 $Stock"
} else {
    Write-Host "  ⚠️ 没找到 mysql.exe，无法重置库存和清理历史记录；若脚本报错请手动执行：" -ForegroundColor Yellow
    Write-Host "     delete from seckill_order where userId like '$UserPrefix%';" -ForegroundColor Yellow
    Write-Host "     update seckill set seckillStock = $Stock where seckillId = $SeckillId;" -ForegroundColor Yellow
}
$count = Invoke-RestMethod -Uri "$BaseUrl/api/seckill/resetStock" -Method POST -Headers $headers0 -ContentType $ContentType
Write-Host "  已把 $count 个活动的库存同步到 Redis，并清空 Redis 里的已购集合"

$list = Get-SeckillList $headers0
$target = $list | Where-Object { $_.seckillId -eq $SeckillId }
if (-not $target) { Write-Host "活动 $SeckillId 不存在" -ForegroundColor Red; exit 1 }
$stockBefore = $target.redisStock
Write-Host "  活动 $SeckillId ：$($target.food.foodName)  秒杀价 ¥$($target.seckillPrice)  当前库存 $stockBefore"

# 记下开抢前的最大流水号：最后只统计"本次新增"的秒杀记录，避免把历史数据算进来
$soIdBaseline = Invoke-SqlScalar "select ifnull(max(soId),0) from seckill_order where seckillId=$SeckillId;"
if ($null -ne $soIdBaseline) { Write-Host "  基线：已有秒杀流水号 up to $soIdBaseline" }

# 3. 并发抢购
if ($Mode -eq "repeat") {
    Write-Step "3. 同一个用户并发请求 $Users 次（模拟连点/脚本刷单）"
    $sendTokens = @()
    for ($i = 1; $i -le $Users; $i++) { $sendTokens += $tokens[0] }
    $expectSuccess = 1
} else {
    Write-Step "3. $Users 个用户同时抢购（库存 $stockBefore 件）"
    $sendTokens = $tokens
    $expectSuccess = $stockBefore
}

$result = Invoke-ConcurrentSeckill $sendTokens "$BaseUrl/api/seckill/do/$SeckillId"
Write-Host "  全部请求返回耗时：$($result.ElapsedMs) ms"

$parsed = @($result.Bodies | ForEach-Object { $_ | ConvertFrom-Json })
$successCount = @($parsed | Where-Object { $_.status -eq "QUEUING" -or $_.status -eq "SUCCESS" }).Count
$failedCount = @($parsed | Where-Object { $_.status -eq "FAILED" }).Count

Write-Host ""
Write-Host "  抢购结果分布：" -ForegroundColor Yellow
$parsed | Group-Object message | Sort-Object Count -Descending | ForEach-Object {
    Write-Host ("    {0,3} 次 -> {1}" -f $_.Count, $_.Name)
}
Write-Host ("  成功 {0} 次，失败 {1} 次" -f $successCount, $failedCount)

# 4. 等异步下单完成（走 MQ 时订单是消费者异步生成的）
Write-Step "4. 等待 5 秒，让 RabbitMQ 消费者把订单落库"
Start-Sleep -Seconds 5

$listAfter = Get-SeckillList $headers0
$stockAfter = ($listAfter | Where-Object { $_.seckillId -eq $SeckillId }).redisStock

# 5. 数据库校验
$dbStock = $null
$dbOrderCount = $null
if (-not $SkipDbCheck) {
    if (Get-MysqlExe) {
        Write-Step "5. 数据库校验"
        $dbStock = Invoke-SqlScalar "select seckillStock from seckill where seckillId=$SeckillId;"
        # 只统计本次新增的成功记录
        $dbOrderCount = Invoke-SqlScalar "select count(*) from seckill_order where seckillId=$SeckillId and status=1 and soId > $soIdBaseline;"
        Write-Host "  数据库剩余库存：$dbStock"
        Write-Host "  本次新增秒杀成功记录数：$dbOrderCount"
    } else {
        Write-Host "（没找到 mysql.exe，跳过数据库校验）" -ForegroundColor DarkGray
    }
}

# 6. 结论
Write-Step "6. 结论"
$pass = $true

if ($successCount -eq $expectSuccess) {
    Write-Host "  ✅ 成功人数 = $expectSuccess（符合预期）" -ForegroundColor Green
} else {
    Write-Host "  ❌ 成功人数 = $successCount，期望 $expectSuccess" -ForegroundColor Red
    $pass = $false
}

if ($stockAfter -lt 0) {
    Write-Host "  ❌ Redis 库存变成负数：$stockAfter（超卖！）" -ForegroundColor Red
    $pass = $false
} else {
    Write-Host "  ✅ Redis 库存：$stockBefore -> $stockAfter（未出现负数）" -ForegroundColor Green
}

if ($null -ne $dbStock) {
    if ([int]$dbStock -lt 0) {
        Write-Host "  ❌ 数据库库存为负数：$dbStock（超卖！）" -ForegroundColor Red
        $pass = $false
    } else {
        Write-Host "  ✅ 数据库库存：$dbStock（未超卖）" -ForegroundColor Green
    }
    if ([int]$dbOrderCount -eq $successCount) {
        Write-Host "  ✅ 落库的秒杀成功记录数 = $dbOrderCount（与响应一致，没有丢单/重复）" -ForegroundColor Green
    } else {
        Write-Host "  ❌ 落库记录数 = $dbOrderCount，与响应成功数 $successCount 不一致" -ForegroundColor Red
        $pass = $false
    }
}

if ($Mode -eq "repeat" -and $successCount -eq 1) {
    Write-Host "  ✅ 同一用户并发 $Users 次只成功 1 次（限购生效）" -ForegroundColor Green
}

Write-Host ""
if ($pass) {
    Write-Host "🎉 测试通过：没有超卖，也没有重复购买" -ForegroundColor Green
} else {
    Write-Host "⚠️ 测试未通过，请检查上面的红色项" -ForegroundColor Red
    exit 1
}
