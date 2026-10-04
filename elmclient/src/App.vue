<template>
  <router-view/>
  <FloatingChat/>
</template>

<script setup>
import { provide } from "vue";
import axios from 'axios'
import FloatingChat from "@/components/FloatingChat.vue";

// 设置 axios 基础地址指向后端服务
axios.defaults.baseURL = 'http://localhost:8080/';

// 请求拦截器：本地有 token 就自动带上 Authorization 头，各个页面不用重复写
axios.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token');
  if (token) {
    config.headers.Authorization = 'Bearer ' + token;
  }
  return config;
});

// 响应拦截器：token 无效或过期（401）时统一清理登录状态并回到登录页
axios.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response && error.response.status === 401) {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('user');
        alert('登录已过期，请重新登录');
        window.location.hash = '#/login';
      }
      return Promise.reject(error);
    }
);

provide('axios', axios);
</script>

<!-- 这里是共通样式，适用于所有组件，所以不要加scoped -->
<style>
html,
body,
div,
span,
h1,
h2,
h3,
h4,
h5,
h6,
ul,
ol,
li,
p {
  margin: 0;
  padding: 0;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  font-family: "微软雅黑";
}

ul,
ol {
  list-style: none;
}

a {
  text-decoration: none;
}
</style>
