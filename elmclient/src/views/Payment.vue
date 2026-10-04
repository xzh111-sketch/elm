<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>在线支付</p>
    </header>

    <!-- 订单信息部分 -->
    <h3>订单信息：</h3>
    <div class="order-info">
      <p>
        {{ orders.business.businessName }}
        <i class="fa fa-caret-down" @click="detailetShow"></i>
      </p>
      <p>&#165;{{ orders.orderTotal }}</p>
    </div>

    <!-- 订单明细部分 -->
    <ul class="order-detailet" v-show="isShowDetailet">
      <li v-for="item in (orders.orderDetailet || [])" :key="item.odId">
        <p>{{ item.food.foodName }} x {{ item.quantity }}</p>
        <p>&#165;{{ item.food.foodPrice * item.quantity }}</p>
      </li>
      <li>
        <p>配送费</p>
        <p>&#165;{{ orders.business.deliveryPrice }}</p>
      </li>
    </ul>

    <!-- 支付方式部分 -->
    <ul class="payment-type">
      <li>
        <img src="../assets/alipay.png" />
        <i class="fa fa-check-circle"></i>
      </li>
      <li>
        <img src="../assets/wechat.png" />
      </li>
    </ul>
    <div class="payment-button">
      <button @click="confirmPayment">确认支付</button>
    </div>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
import Footer from "@/components/Footer.vue";
import qs from "qs";
import {useRouter, useRoute} from "vue-router";
import {ref, inject, onMounted, onUnmounted} from "vue";
import {getSessionStorage} from "@/common";

const axios = inject("axios");
const router = useRouter();
const route = useRoute();

const orderId = route.query.orderId;
const orders = ref({business: {}});
const isShowDetailet = ref(false);

const init = () => {
  if (!orderId) {
    alert("订单ID不存在，请重新选择订单！");
    router.push("/orderList");
    return;
  }

  axios
      .post(
          "OrdersController/getOrdersById",
          qs.stringify({
            orderId: orderId,
          })
      )
      .then((response) => {
        orders.value = response.data;
      })
      .catch((error) => {
        console.error("加载订单失败：", error);
      });
}
init();
onMounted(() => {
  //缓存支付页面
  history.pushState(null, null, document.URL);
  window.onpopstate = () => {
    router.push({
      path: "/index"
    });
  }
});
onUnmounted(() => {
  window.onpopstate = null;
})
const detailetShow = () => {
  isShowDetailet.value = !isShowDetailet.value;
}

// 确认支付：向后端要一段支付宝收银台表单，提交后浏览器跳到沙箱收银台。
// 注意：这里不再直接改订单状态——订单状态只由支付宝的异步通知去改。
const confirmPayment = async () => {
  if (!orderId) {
    alert("订单ID不存在！");
    return;
  }

  try {
    // 记住订单号：支付完跳回 PayResult 页时用它查订单状态
    sessionStorage.setItem("payingOrderId", orderId);

    const response = await axios.get("api/alipay/pay", {
      params: { orderId: orderId },
      responseType: "text"
    });

    // 后端返回的是一段自带 form 的 HTML
    const container = document.createElement("div");
    container.innerHTML = response.data;
    const form = container.querySelector("form");
    if (!form) {
      alert("生成支付页面失败，请检查后端的支付宝配置");
      return;
    }
    document.body.appendChild(container);
    form.submit();   // 提交表单 -> 跳到支付宝沙箱收银台
  } catch (error) {
    console.error("发起支付失败：", error);
    alert("发起支付失败，请检查后端支付宝配置（APPID / 应用私钥 / 网关）");
  }
};
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
  width: 100%;
  height: 100%;
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

/****************** 订单信息部分 ******************/
.wrapper h3 {
  margin-top: 12vw;
  box-sizing: border-box;
  padding: 4vw 4vw 0;

  font-size: 4vw;
  font-weight: 300;
  color: #999;
}

.wrapper .order-info {
  box-sizing: border-box;
  padding: 4vw;
  font-size: 4vw;
  color: #666;

  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrapper .order-info p:last-child {
  color: orangered;
}

/****************** 订单明细部分 ******************/
.wrapper .order-detailet {
  width: 100%;
}

.wrapper .order-detailet li {
  width: 100%;
  box-sizing: border-box;
  padding: 1vw 4vw;

  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrapper .order-detailet li p {
  font-size: 3vw;
  color: #666;
}

/****************** 支付方式部分 ******************/
.wrapper .payment-type {
  width: 100%;
}

.wrapper .payment-type li {
  width: 100%;
  box-sizing: border-box;
  padding: 4vw;

  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrapper .payment-type li img {
  width: 33vw;
  height: 8.9vw;
}

.wrapper .payment-type li .fa-check-circle {
  font-size: 5vw;
  color: #38ca73;
}

.wrapper .payment-button {
  width: 100%;
  box-sizing: border-box;
  padding: 4vw;
}

.wrapper .payment-button button {
  width: 100%;
  height: 10vw;
  border: none;
  /*去掉外轮廓线*/
  outline: none;
  border-radius: 4px;
  background-color: #38ca73;
  color: #fff;
}
</style>
