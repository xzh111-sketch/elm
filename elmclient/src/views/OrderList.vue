<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>我的订单</p>
    </header>

    <!-- 状态切换标签页 -->
    <div class="order-tabs">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'all' }"
        @click="switchTab('all')"
      >
        全部
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'unpaid' }"
        @click="switchTab('unpaid')"
      >
        未支付
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'paid' }"
        @click="switchTab('paid')"
      >
        已支付
      </div>
    </div>

    <!-- 订单列表部分 -->
    <ul class="order" v-if="displayedOrders.length > 0">
      <li v-for="item in displayedOrders" :key="item.orderId">
        <div class="order-info">
          <p>
            {{ item.business.businessName }}
            <!-- 展开/收起图标，根据状态切换 -->
            <i
                class="fa"
                :class="item.isShowDetailet ? 'fa-caret-up' : 'fa-caret-down'"
                @click="detailetShow(item)"
                style="cursor: pointer; margin-left: 2vw;"
            ></i>
          </p>
          <div class="order-info-right">
            <p>&#165;{{ item.orderTotal }}</p>
            <!-- 只有未支付订单才显示"去支付"按钮 -->
            <div 
              v-if="getOrderState(item) === 0" 
              class="order-info-right-icon" 
              @click="toPayment(item.orderId)"
            >
              去支付
            </div>
          </div>
        </div>
        <ul class="order-detailet" v-show="item.isShowDetailet">
          <li v-for="odItem in (item.orderDetailet || item.list || [])" :key="odItem.odId">
            <p>{{ odItem.food.foodName }} x {{ odItem.quantity }}</p>
            <p>&#165;{{ (odItem.food.foodPrice * odItem.quantity).toFixed(2) }}</p>
          </li>
          <li>
            <p>配送费</p>
            <p>&#165;{{ item.business.deliveryPrice.toFixed(2) }}</p>
          </li>
        </ul>
      </li>
    </ul>
    <!-- 订单为空时的提示 -->
    <div v-if="displayedOrders.length === 0 && loaded" class="empty-orders">
      <p>{{ getEmptyMessage() }}</p>
    </div>

    <!-- 分页：加载更多 -->
    <div class="load-more" v-if="displayedOrders.length > 0">
      <button v-if="hasMore" @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? "加载中..." : "加载更多" }}
      </button>
      <p v-else>已经到底啦，共 {{ total }} 笔订单</p>
    </div>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
//引入依赖并建立运行环境
import qs from "qs";
import {ref, inject, onMounted, computed, reactive} from "vue";
import {useRouter, useRoute} from "vue-router";
import {getSessionStorage} from "@/common";
import Footer from "@/components/Footer.vue";


const axios = inject("axios"); // 注入全局axios
const router = useRouter();
const route = useRoute();

// 每个标签页各自维护分页状态：orderState = null 表示不筛选（全部）
// 状态筛选下推到数据库，这样"未支付"标签的总数和分页才是精确的
const PAGE_SIZE = 5;
const tabs = reactive({
  all:    { orderState: null, list: [], pageNum: 0, total: 0, hasMore: false, loading: false, loaded: false },
  unpaid: { orderState: 0,    list: [], pageNum: 0, total: 0, hasMore: false, loading: false, loaded: false },
  paid:   { orderState: 1,    list: [], pageNum: 0, total: 0, hasMore: false, loading: false, loaded: false },
});
const activeTab = ref("all"); // 当前选中的标签：'all' | 'unpaid' | 'paid'
const currentTab = computed(() => tabs[activeTab.value]);
const displayedOrders = computed(() => currentTab.value.list); // 当前标签要显示的订单
const total = computed(() => currentTab.value.total);          // 当前标签的总条数
const hasMore = computed(() => currentTab.value.hasMore);      // 当前标签是否还有下一页
const loadingMore = computed(() => currentTab.value.loading);  // 当前标签是否正在加载
const loaded = computed(() => currentTab.value.loaded);        // 当前标签是否已加载过

// 获取登录用户信息
const user = getSessionStorage("user");
// 校验用户是否登录
const checkUserLogin = () => {
  if (!user || !user.userId) {
    alert("请先登录！");
    router.push("/login"); // 跳转到登录页
    return false;
  }
  return true;
};

// 加载某个标签的一页订单：reset=true 表示从第一页开始加载
const loadPage = async (tabKey, reset = false) => {
  const tab = tabs[tabKey];
  if (tab.loading) return;
  tab.loading = true;
  const pageNum = reset ? 1 : tab.pageNum + 1;
  try {
    const params = {pageNum: pageNum, pageSize: PAGE_SIZE};
    // 状态筛选交给数据库，而不是在前端把已加载的数据再过滤一遍
    if (tab.orderState !== null) {
      params.orderState = tab.orderState;
    }
    // 后端返回分页结构：{list, total, pageNum, pageSize, hasMore}
    const response = await axios.post(
        "OrdersController/listOrdersByUserId",
        qs.stringify(params)
    );
    const data = response.data || {};
    // 给每条订单加上"明细默认收起"的标记
    const list = (data.list || []).map(order => ({...order, isShowDetailet: false}));
    tab.list = reset ? list : tab.list.concat(list);
    tab.pageNum = pageNum;
    tab.total = data.total || 0;
    tab.hasMore = !!data.hasMore;
  } catch (error) {
    console.error("加载订单列表失败：", error);
    alert("订单数据加载失败，请稍后重试！");
  } finally {
    tab.loading = false;
    tab.loaded = true; // 标记该标签已加载过
  }
};

// 点"加载更多"：给当前标签加载下一页
const loadMore = () => loadPage(activeTab.value);

//初始化函数：加载订单数据
const init = async () => {
  // 先校验登录状态
  if (!checkUserLogin()) return;
  await loadPage("all", true);
};

//订单明细展开/收起
const detailetShow = (orderItem) => {
  orderItem.isShowDetailet = !orderItem.isShowDetailet;
};

//跳转到支付页面
const toPayment = (orderId) => {
  router.push({
    path: "/payment",
    query: {
      orderId: orderId
    }
  });
};

//切换标签页：该标签第一次打开时才去数据库加载（各自独立分页）
const switchTab = (tab) => {
  activeTab.value = tab;
  if (!tabs[tab].loaded) {
    loadPage(tab, true);
  }
};

//获取订单状态
const getOrderState = (order) => {
  return order.orderState !== undefined ? order.orderState : order.orderStatus;
};

//获取空订单提示信息
const getEmptyMessage = () => {
  if (activeTab.value === "all") {
    return "暂无订单";
  } else if (activeTab.value === "unpaid") {
    return "暂无未支付订单";
  } else if (activeTab.value === "paid") {
    return "暂无已支付订单";
  }
  return "暂无订单";
};

// 组件挂载后执行初始化
onMounted(() => {
  init();
});
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
  width: 100%;
  height: 100%;
  padding-bottom: 14vw; /* 给底部菜单留空间（Footer高度14vw） */
  box-sizing: border-box;
  overflow-x: hidden;
}

/****************** header部分 ******************/
.wrapper header {
  width: 100%;
  height: 12vw;
  background-color: #0097ff;
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

/****************** 状态切换标签页 ******************/
.wrapper .order-tabs {
  width: 100%;
  height: 12vw;
  margin-top: 12vw;
  background-color: #fff;
  display: flex;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 12vw;
  z-index: 999;
}

.wrapper .order-tabs .tab-item {
  flex: 1;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 4vw;
  color: #666;
  cursor: pointer;
  user-select: none;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.wrapper .order-tabs .tab-item.active {
  color: #0097ff;
  border-bottom-color: #0097ff;
  font-weight: 600;
}

.wrapper .order-tabs .tab-item:active {
  background-color: #f5f5f5;
}

/****************** 历史订单列表部分 ******************/
.wrapper .empty-orders {
  margin-top: 2vw;
  padding: 10vw 4vw;
  text-align: center;
  color: #999;
}

.wrapper .empty-orders p {
  font-size: 4vw;
  margin: 0;
}

.wrapper .order {
  width: 100%;
  margin: 0;
  padding: 0;
  list-style: none;
}

.wrapper .order li {
  width: 100%;
  border-bottom: 1px solid #eee;
}

.wrapper .order li .order-info {
  box-sizing: border-box;
  padding: 2vw 4vw;
  font-size: 4vw;
  color: #666;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrapper .order li .order-info .order-info-right {
  display: flex;
  align-items: center;
}

.wrapper .order li .order-info .order-info-right .order-info-right-icon {
  background-color: #f90;
  color: #fff;
  border-radius: 3px;
  margin-left: 2vw;
  padding: 1vw 2vw;
  user-select: none;
  cursor: pointer;
  font-size: 3.5vw;
}

.wrapper .order li .order-detailet {
  width: 100%;
  margin: 0;
  padding: 0;
  list-style: none;
  background-color: #f8f8f8;
}

.wrapper .order li .order-detailet li {
  width: 100%;
  box-sizing: border-box;
  padding: 1.5vw 4vw;
  color: #666;
  font-size: 3.5vw;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: none;
}

/****************** 分页：加载更多 ******************/
.wrapper .load-more {
  width: 100%;
  padding: 2vw 4vw 6vw 4vw;
  box-sizing: border-box;
  text-align: center;
}

.wrapper .load-more button {
  width: 100%;
  height: 10vw;
  background-color: #fff;
  color: #0097ff;
  border: solid 1px #ddd;
  border-radius: 4px;
  font-size: 3.8vw;
  cursor: pointer;
  user-select: none;
}

.wrapper .load-more button:disabled {
  color: #999;
}

.wrapper .load-more p {
  font-size: 3.4vw;
  color: #999;
  margin: 0;
}
</style>
