<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>我的</p>
    </header>

    <!-- 用户信息部分 -->
    <div class="user-info" v-if="user">
      <div class="user-avatar" @click="chooseAvatar">
        <img :src="user.userImg || defaultAvatar" alt="头像" />
        <div class="avatar-mask" v-if="selectingAvatar">
          <i class="fa fa-camera"></i>
        </div>
      </div>
      <div class="user-details">
        <p class="user-name">{{ user.userName || user.userId }}</p>
        <p class="user-id">{{ user.userId }}</p>
      </div>
    </div>

    <!-- 隐藏的图片选择框 -->
    <input
        type="file"
        accept="image/*"
        ref="fileInput"
        style="display: none"
        @change="onFileChange"
        @cancel="onPickCancel"
    />

    <!-- 功能菜单部分 -->
    <div class="menu-list">
      <!-- 收货地址 -->
      <div class="menu-item" @click="toUserAddress">
        <div class="menu-item-left">
          <i class="fa fa-map-marker"></i>
          <span>收货地址</span>
        </div>
        <i class="fa fa-angle-right"></i>
      </div>

      <!-- 我的订单 -->
      <div class="menu-item" @click="toOrderList">
        <div class="menu-item-left">
          <i class="fa fa-file-text-o"></i>
          <span>我的订单</span>
        </div>
        <i class="fa fa-angle-right"></i>
      </div>

      <!-- 个人信息 -->
      <div class="menu-item" @click="toUserInfo">
        <div class="menu-item-left">
          <i class="fa fa-user"></i>
          <span>个人信息</span>
        </div>
        <i class="fa fa-angle-right"></i>
      </div>

      <!-- 退出登录 -->
      <div class="menu-item logout-item" @click="logout">
        <div class="menu-item-left">
          <i class="fa fa-sign-out"></i>
          <span>退出登录</span>
        </div>
        <i class="fa fa-angle-right"></i>
      </div>
    </div>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
import Footer from "@/components/Footer.vue";
import qs from "qs";
import { ref, inject, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import {
  getSessionStorage,
  setSessionStorage,
  removeSessionStorage,
} from "@/common";

const router = useRouter();
const axios = inject("axios");
const fileInput = ref(null);
// 是否正在选择图片（选择文件时显示摄像头图标）
const selectingAvatar = ref(false);

// 从 sessionStorage 中获取后端返回的用户对象
const user = ref(getSessionStorage("user"));

// 占位图片（用户没有头像时展示）
const defaultAvatar =
    "data:image/svg+xml;charset=utf-8," +
    encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">' +
        '<circle cx="100" cy="100" r="100" fill="#c9c9c9"/>' +
        '<circle cx="100" cy="76" r="34" fill="#ffffff"/>' +
        '<path d="M28 192 q72 -66 144 0 z" fill="#ffffff"/>' +
        "</svg>"
    );

// 点击头像，打开系统图片选择框
const chooseAvatar = () => {
  if (!user.value) {
    alert("请先登录");
    router.push("/login");
    return;
  }
  selectingAvatar.value = true;
  fileInput.value.click();
};

// 取消选择文件时隐藏摄像头图标
const onPickCancel = () => {
  selectingAvatar.value = false;
};

// 部分浏览器不触发 cancel 事件，窗口重新获得焦点时也视为选择框已关闭
const handleWindowFocus = () => {
  selectingAvatar.value = false;
};

onMounted(() => {
  window.addEventListener("focus", handleWindowFocus);
});

onUnmounted(() => {
  window.removeEventListener("focus", handleWindowFocus);
});

// 将图片压缩后转为 base64 数据网址
const compressImage = (file, maxSize = 300) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (event) => {
      const image = new Image();
      image.onload = () => {
        let { width, height } = image;
        const scale = Math.min(1, maxSize / Math.max(width, height));
        width = Math.round(width * scale);
        height = Math.round(height * scale);
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        canvas.getContext("2d").drawImage(image, 0, 0, width, height);
        resolve(canvas.toDataURL("image/jpeg", 0.8));
      };
      image.onerror = () => reject(new Error("图片解析失败"));
      image.src = event.target.result;
    };
    reader.onerror = () => reject(new Error("图片读取失败"));
    reader.readAsDataURL(file);
  });
};

// 选中图片后：压缩 -> 预览回显 -> 更新数据库 -> 同步 sessionStorage
const onFileChange = async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  if (!file.type.startsWith("image/")) {
    alert("请选择图片文件");
    event.target.value = "";
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    alert("图片大小不能超过5MB");
    event.target.value = "";
    return;
  }

  const oldImg = user.value.userImg;
  try {
    const dataUrl = await compressImage(file, 300);
    user.value.userImg = dataUrl; // 先在页面上实时预览

    const response = await axios.post(
        "UserController/updateUserImg",
        qs.stringify({
          userId: user.value.userId,
          userImg: dataUrl,
        })
    );

    if (response.data > 0) {
      setSessionStorage("user", user.value);
      alert("头像修改成功");
    } else {
      user.value.userImg = oldImg;
      alert("头像修改失败，请稍后重试");
    }
  } catch (error) {
    console.error("修改头像失败", error);
    user.value.userImg = oldImg;
    alert("头像修改失败，请稍后重试");
  } finally {
    selectingAvatar.value = false;
    event.target.value = "";
  }
};

// 跳转到收货地址页面
const toUserAddress = () => {
  router.push({ path: "/userAddress" });
};

// 跳转到我的订单页面
const toOrderList = () => {
  router.push({ path: "/orderList" });
};

// 跳转到个人信息页面
const toUserInfo = () => {
  router.push({ path: "/userInfo" });
};

// 退出登录
const logout = () => {
  if (confirm("确定要退出登录吗？")) {
    // 清除 sessionStorage 中的用户信息
    removeSessionStorage("user");
    sessionStorage.removeItem("token");
    // 跳转到首页
    router.push({ path: "/index" });
  }
};
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
  width: 100%;
  height: 100%;
  padding-bottom: 14vw; /* 给底部菜单留空间 */
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

.wrapper header p {
  margin: 0;
  padding: 0;
}

/****************** 用户信息部分 ******************/
.wrapper .user-info {
  width: 100%;
  margin-top: 12vw;
  padding: 6vw 4vw;
  background-color: #0097ff;
  display: flex;
  align-items: center;
  color: #fff;
}

.wrapper .user-info .user-avatar {
  width: 20vw;
  height: 20vw;
  border-radius: 50%;
  overflow: hidden;
  background-color: #fff;
  margin-right: 4vw;
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
}

.wrapper .user-info .user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wrapper .user-info .user-avatar .avatar-mask {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.35);
  color: #fff;
  font-size: 5vw;
  display: flex;
  justify-content: center;
  align-items: center;
}

.wrapper .user-info .user-details {
  flex: 1;
}

.wrapper .user-info .user-details .user-name {
  font-size: 5vw;
  font-weight: 600;
  margin: 0 0 2vw 0;
}

.wrapper .user-info .user-details .user-id {
  font-size: 3.5vw;
  margin: 0;
  opacity: 0.8;
}

/****************** 功能菜单部分 ******************/
.wrapper .menu-list {
  width: 100%;
  margin-top: 4vw;
  background-color: #fff;
}

.wrapper .menu-item {
  width: 100%;
  padding: 4vw;
  box-sizing: border-box;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  user-select: none;
}

.wrapper .menu-item:active {
  background-color: #f5f5f5;
}

.wrapper .menu-item:last-child {
  border-bottom: none;
}

.wrapper .menu-item .menu-item-left {
  display: flex;
  align-items: center;
}

.wrapper .menu-item .menu-item-left i:first-child {
  font-size: 5vw;
  color: #0097ff;
  margin-right: 4vw;
  width: 6vw;
  text-align: center;
}

.wrapper .menu-item .menu-item-left span {
  font-size: 4vw;
  color: #333;
}

.wrapper .menu-item .fa-angle-right {
  font-size: 4vw;
  color: #999;
}

.wrapper .menu-item.logout-item {
  margin-top: 4vw;
  border-top: 1px solid #eee;
}

.wrapper .menu-item.logout-item .menu-item-left i:first-child {
  color: #ff4444;
}
</style>
