<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>支付结果</p>
    </header>

    <!-- 结果卡片 -->
    <div class="result-card">
      <i class="fa" :class="iconClass"></i>
      <p class="result-title">{{ title }}</p>
      <p class="result-desc">{{ desc }}</p>
      <p class="result-order" v-if="orderId">订单号：{{ orderId }}</p>
      <p class="result-order" v-if="orderTotal">订单金额：&#165;{{ orderTotal }}</p>
    </div>

    <!-- 操作按钮 -->
    <div class="result-buttons">
      <button @click="toOrderList">查看我的订单</button>
      <button class="plain" @click="toIndex">返回首页</button>
    </div>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
import Footer from "@/components/Footer.vue";
import qs from "qs";
import { ref, inject, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

const axios = inject("axios");
const route = useRoute();
const router = useRouter();

// 订单号：优先取 URL 参数（支付宝回跳时带的），兜底用跳转前存下的
const resolveOrderId = () =>
    route.query.out_trade_no ||
    route.query.orderId ||
    sessionStorage.getItem("payingOrderId") ||
    "";
const orderId = ref(resolveOrderId());
const orderTotal = ref("");
// checking：正在确认；paid：已支付；unpaid：没查到已支付
const state = ref("checking");
const title = ref("正在确认支付结果…");
const desc = ref("支付宝的异步通知可能稍有延迟，请稍等几秒");
const iconClass = ref("fa-spinner fa-spin");

let attempts = 0;

// 开始（或重新开始）确认支付结果的流程
const startCheck = () => {
  attempts = 0;
  state.value = "checking";
  title.value = "正在确认支付结果…";
  desc.value = "支付宝的异步通知可能稍有延迟，请稍等几秒";
  iconClass.value = "fa-spinner fa-spin";
  checkOrderState();
};

// 同一路由只变 query 时组件不会重建，所以监听地址变化重新查询
watch(() => route.fullPath, () => {
  orderId.value = resolveOrderId();
  startCheck();
});

// 查询订单状态：因为订单状态由支付宝异步通知修改，可能比浏览器跳转慢一点，
// 所以这里轮询几次，查不到就提示用户稍后到订单列表查看
const checkOrderState = async () => {
  if (!orderId.value) {
    state.value = "unpaid";
    title.value = "未找到订单信息";
    desc.value = "请到“我的订单”里查看支付结果";
    iconClass.value = "fa-question-circle";
    return;
  }

  try {
    const response = await axios.post(
        "OrdersController/getOrdersById",
        qs.stringify({ orderId: orderId.value })
    );
    const order = response.data || {};
    orderTotal.value = order.orderTotal;

    if (order.orderState === 1) {
      state.value = "paid";
      title.value = "支付成功";
      desc.value = "感谢您的下单，商家正在为您准备餐品";
      iconClass.value = "fa-check-circle";
      sessionStorage.removeItem("payingOrderId");
      return;
    }

    attempts++;
    if (attempts < 5) {
      desc.value = "正在确认支付结果…（第 " + attempts + " 次查询）";
      setTimeout(checkOrderState, 2000);
    } else {
      state.value = "unpaid";
      title.value = "支付确认中";
      desc.value = "如果你已经付款成功，订单状态会在收到支付宝通知后自动更新";
      iconClass.value = "fa-clock-o";
    }
  } catch (error) {
    console.error("查询订单状态失败：", error);
    state.value = "unpaid";
    title.value = "查询结果失败";
    desc.value = "请到“我的订单”里查看支付结果";
    iconClass.value = "fa-question-circle";
  }
};

onMounted(() => {
  startCheck();
});

const toOrderList = () => {
  router.push("/orderList");
};

const toIndex = () => {
  router.push("/index");
};
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

.wrapper header p {
  margin: 0;
}

/****************** 结果卡片 ******************/
.wrapper .result-card {
  width: 100%;
  margin-top: 12vw;
  padding: 10vw 6vw;
  box-sizing: border-box;
  background-color: #fff;
  text-align: center;
}

.wrapper .result-card i {
  font-size: 18vw;
  color: #0097ff;
}

.wrapper .result-card i.fa-check-circle {
  color: #38ca73;
}

.wrapper .result-card i.fa-clock-o,
.wrapper .result-card i.fa-question-circle {
  color: #fec80e;
}

.wrapper .result-card .result-title {
  font-size: 5.5vw;
  font-weight: 600;
  color: #333;
  margin: 3vw 0 2vw 0;
}

.wrapper .result-card .result-desc {
  font-size: 3.6vw;
  color: #888;
  line-height: 1.6;
  margin: 0 0 3vw 0;
}

.wrapper .result-card .result-order {
  font-size: 3.6vw;
  color: #666;
  margin: 1vw 0 0 0;
}

/****************** 按钮 ******************/
.wrapper .result-buttons {
  width: 100%;
  padding: 8vw 6vw 0 6vw;
  box-sizing: border-box;
}

.wrapper .result-buttons button {
  width: 100%;
  height: 12vw;
  background-color: #0097ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 4.2vw;
  cursor: pointer;
  user-select: none;
}

.wrapper .result-buttons button:active {
  background-color: #0080e0;
}

.wrapper .result-buttons button.plain {
  margin-top: 3vw;
  background-color: #fff;
  color: #666;
  border: solid 1px #ddd;
}
</style>
