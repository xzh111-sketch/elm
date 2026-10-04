<template>
  <ul class="footer">
    <li @click="toIndex">
      <i class="fa fa-home"></i>
      <p>首页</p>
    </li>
    <li @click="toSeckill">
      <i class="fa fa-compass"></i>
      <p>发现</p>
    </li>
    <li @click="toOrderList">
      <i class="fa fa-file-text-o"></i>
      <p>订单</p>
    </li>
    <li @click="toProfile">
      <i class="fa fa-user-o"></i>
      <p>我的</p>
    </li>
  </ul>
</template>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

const toIndex = () => {
  router.push({ path: "/index" });
};
const toOrderList = () => {
  router.push({ path: "/orderList" });
};
const toProfile = () => {
  router.push({ path: "/profile" });
};
// “发现”作为限时秒杀的入口（AI 客服入口是右侧那个悬浮球）
const toSeckill = () => {
  router.push({ path: "/seckill" });
};
</script>

<style scoped>
.footer {
  width: 100%;
  height: 14vw;
  border-top: solid 1px #ddd;
  background-color: #fff;
  margin: 0;
  padding: 0;
  list-style: none;

  position: fixed !important;
  left: 0 !important;
  bottom: 0 !important;
  z-index: 9999 !important;

  display: flex;
  justify-content: space-around;
  align-items: center;
}

.footer li {
  /*li本身的尺寸完全由内容撑起*/
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  color: #999;
  user-select: none;
  cursor: pointer;
}

.footer li p {
  font-size: 2.8vw;
  margin: 0;
}

.footer li i {
  font-size: 5vw;
}
</style>
