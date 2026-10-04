<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>限时秒杀</p>
    </header>

    <!-- 说明 -->
    <div class="tip">
      <i class="fa fa-bolt"></i>
      Redis 预扣库存 + RabbitMQ 异步下单 · 每人限购 1 件
    </div>

    <!-- 秒杀商品列表 -->
    <ul class="seckill-list">
      <li v-for="item in list" :key="item.seckillId">
        <img :src="item.food.foodImg" alt="商品图片" />
        <div class="info">
          <p class="name">{{ item.food.foodName }}</p>
          <p class="business">{{ item.business.businessName }}</p>
          <p class="price">
            <span class="now">&#165;{{ item.seckillPrice }}</span>
            <span class="old">&#165;{{ item.originalPrice }}</span>
          </p>
          <p class="stock">
            剩余 <b>{{ item.redisStock }}</b> 件
            <span v-if="item.redisStock <= 0" class="sold-out">已抢光</span>
          </p>
        </div>
        <button
            class="buy"
            :class="{ done: stateOf(item).status === 'SUCCESS' }"
            :disabled="loadingId === item.seckillId || !canBuy(item)"
            @click="onButtonClick(item)"
        >
          {{ buttonText(item) }}
        </button>
      </li>
    </ul>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
import Footer from "@/components/Footer.vue";
import { ref, inject, onMounted } from "vue";
import { useRouter } from "vue-router";

const axios = inject("axios");
const router = useRouter();

const list = ref([]);          // 秒杀活动列表
const loadingId = ref(null);   // 正在抢购的活动编号
const results = ref({});       // { seckillId: {status, orderId, message} }

// 拉取列表 + 自己的抢购状态
const loadList = async () => {
  try {
    const response = await axios.get("api/seckill/list");
    list.value = response.data || [];
    for (const item of list.value) {
      try {
        const r = await axios.get("api/seckill/result/" + item.seckillId);
        if (r.data && r.data.status && r.data.status !== "NONE") {
          results.value[item.seckillId] = r.data;
        }
      } catch (e) {
        // 单个查询失败不影响整页
      }
    }
  } catch (error) {
    console.error("加载秒杀列表失败：", error);
    alert("秒杀列表加载失败，请确认后端和 Redis 已启动");
  }
};

const stateOf = (item) => results.value[item.seckillId] || {};

// 抢购成功后变成"去支付"，排队中不能重复点
const canBuy = (item) => {
  const state = stateOf(item).status;
  if (state === "SUCCESS" || state === "QUEUING") {
    return true;
  }
  return item.redisStock > 0;
};

const buttonText = (item) => {
  if (loadingId.value === item.seckillId) {
    return "抢购中";
  }
  const state = stateOf(item).status;
  if (state === "SUCCESS") {
    return "去支付";
  }
  if (state === "QUEUING") {
    return "排队中";
  }
  return item.redisStock > 0 ? "立即抢" : "已抢光";
};

const onButtonClick = (item) => {
  const state = stateOf(item).status;
  if (state === "SUCCESS") {
    // 已经抢到了，跳到支付页
    router.push({ path: "/payment", query: { orderId: stateOf(item).orderId } });
    return;
  }
  if (state === "QUEUING") {
    alert("订单正在生成中，请稍后到“我的订单”里查看");
    return;
  }
  seckill(item);
};

const seckill = async (item) => {
  if (loadingId.value) {
    return;
  }
  loadingId.value = item.seckillId;
  try {
    const response = await axios.post("api/seckill/do/" + item.seckillId);
    const data = response.data || {};
    if (!data.success) {
      alert(data.message || "抢购失败");
      await loadList();
      return;
    }
    if (data.status === "QUEUING") {
      // 异步下单：轮询结果，最多等 5 秒
      await pollResult(item.seckillId);
    } else {
      results.value[item.seckillId] = data;
      alert(data.message || "抢购成功");
    }
    await loadList();
  } catch (error) {
    console.error("抢购失败：", error);
    alert("抢购失败，请稍后重试");
  } finally {
    loadingId.value = null;
  }
};

// 轮询"排队中 → 抢购成功"
const pollResult = async (seckillId) => {
  for (let i = 0; i < 10; i++) {
    await new Promise(resolve => setTimeout(resolve, 500));
    try {
      const response = await axios.get("api/seckill/result/" + seckillId);
      const data = response.data || {};
      if (data.status === "SUCCESS" || data.status === "FAILED") {
        results.value[seckillId] = data;
        alert(data.status === "SUCCESS" ? "抢购成功，订单已生成！" : (data.message || "抢购失败"));
        return;
      }
    } catch (e) {
      // 忽略单次失败，继续轮询
    }
  }
  results.value[seckillId] = { status: "QUEUING", message: "订单生成中" };
  alert("订单生成中，稍后可在“我的订单”里查看");
};

onMounted(() => {
  loadList();
});
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
  width: 100%;
  height: 100%;
  padding-bottom: 14vw;
  box-sizing: border-box;
  background-color: #f5f5f5;
}

/****************** header部分 ******************/
.wrapper header {
  width: 100%;
  height: 12vw;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 4.8vw;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.wrapper header p {
  margin: 0;
}

/****************** 说明条 ******************/
.wrapper .tip {
  width: 100%;
  margin-top: 12vw;
  padding: 2.5vw 4vw;
  box-sizing: border-box;
  background-color: #fff7e6;
  color: #d46b08;
  font-size: 3.2vw;
}

.wrapper .tip .fa-bolt {
  margin-right: 1.5vw;
}

/****************** 商品列表 ******************/
.wrapper .seckill-list {
  width: 100%;
  margin: 2vw 0 0 0;
  padding: 0;
  list-style: none;
}

.wrapper .seckill-list li {
  width: 100%;
  box-sizing: border-box;
  padding: 3vw 4vw;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}

.wrapper .seckill-list li img {
  width: 22vw;
  height: 22vw;
  border-radius: 2px;
  object-fit: cover;
}

.wrapper .seckill-list li .info {
  flex: 1;
  padding: 0 3vw;
}

.wrapper .seckill-list li .info p {
  margin: 0;
}

.wrapper .seckill-list li .info .name {
  font-size: 4vw;
  color: #333;
  font-weight: 600;
}

.wrapper .seckill-list li .info .business {
  font-size: 3.2vw;
  color: #999;
  margin-top: 1vw;
}

.wrapper .seckill-list li .info .price {
  margin-top: 1.5vw;
}

.wrapper .seckill-list li .info .price .now {
  font-size: 4.6vw;
  color: #ff4d4f;
  font-weight: 700;
}

.wrapper .seckill-list li .info .price .old {
  font-size: 3.2vw;
  color: #bbb;
  text-decoration: line-through;
  margin-left: 2vw;
}

.wrapper .seckill-list li .info .stock {
  font-size: 3.2vw;
  color: #666;
  margin-top: 1vw;
}

.wrapper .seckill-list li .info .stock b {
  color: #ff4d4f;
}

.wrapper .seckill-list li .info .stock .sold-out {
  color: #999;
  margin-left: 1.5vw;
}

.wrapper .seckill-list li button {
  width: 20vw;
  height: 9vw;
  background-color: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 4vw;
  font-size: 3.8vw;
  cursor: pointer;
  user-select: none;
}

.wrapper .seckill-list li button.done {
  background-color: #38ca73;
}

.wrapper .seckill-list li button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>
