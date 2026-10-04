<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>个人信息</p>
    </header>

    <!-- 表单部分 -->
    <ul class="form-box">
      <li>
        <div class="title">手机号码：</div>
        <div class="content">
          <input type="text" v-model="userInfo.userId" disabled placeholder="手机号码" />
        </div>
      </li>
      <li>
        <div class="title">用户名称：</div>
        <div class="content">
          <input type="text" v-model="userInfo.userName" placeholder="用户名称" />
        </div>
      </li>
      <li>
        <div class="title">性别：</div>
        <div class="content" style="font-size: 3vw">
          <input
              type="radio"
              v-model="userInfo.userSex"
              :value="1"
              style="width: 6vw; height: 3.2vw"
          />男
          <input
              type="radio"
              v-model="userInfo.userSex"
              :value="0"
              style="width: 6vw; height: 3.2vw"
          />女
        </div>
      </li>
    </ul>

    <div class="button-save">
      <button @click="saveUserInfo">保存</button>
    </div>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
import Footer from "@/components/Footer.vue";
import qs from "qs";
import { ref, inject, onMounted } from "vue";
import { useRouter } from "vue-router";
import { getSessionStorage, setSessionStorage } from "@/common";

const axios = inject("axios");
const router = useRouter();
const currentUser = getSessionStorage("user");

// 用户信息表单
const userInfo = ref({
  userId: "",
  userName: "",
  userSex: 1,
  password: "", // 保存时需要，但不显示
});

// 初始化：加载当前用户信息
onMounted(() => {
  if (currentUser) {
    userInfo.value = {
      userId: currentUser.userId || "",
      userName: currentUser.userName || "",
      userSex: currentUser.userSex !== undefined ? currentUser.userSex : 1,
      password: currentUser.password || "",
    };
  } else {
    alert("请先登录！");
    router.push("/login");
  }
});

// 保存用户信息
const saveUserInfo = async () => {
  if (!userInfo.value.userName || userInfo.value.userName.trim() === "") {
    alert("用户名称不能为空！");
    return;
  }

  if (userInfo.value.userSex === undefined || userInfo.value.userSex === null) {
    alert("请选择性别！");
    return;
  }

  try {
    // 调用后端接口更新用户信息
    const response = await axios.post(
        "UserController/updateUser",
        qs.stringify({
          userId: userInfo.value.userId,
          userName: userInfo.value.userName,
          userSex: userInfo.value.userSex,
          userImg: currentUser.userImg || "",
        })
    );

    if (response.data > 0) {
      // 更新sessionStorage中的用户信息
      const updatedUser = {
        ...currentUser,
        userName: userInfo.value.userName,
        userSex: userInfo.value.userSex,
      };
      setSessionStorage("user", updatedUser);
      alert("保存成功！");
      router.go(-1); // 返回上一页
    } else {
      alert("保存失败，请稍后重试！");
    }
  } catch (error) {
    console.error("保存用户信息失败：", error);
    alert("保存失败，请稍后重试！");
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

/****************** 表单部分 ******************/
.wrapper .form-box {
  width: 100%;
  margin-top: 12vw;
  padding: 0;
  list-style: none;
  background-color: #fff;
}

.wrapper .form-box li {
  width: 100%;
  box-sizing: border-box;
  padding: 4vw;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}

.wrapper .form-box li:last-child {
  border-bottom: none;
}

.wrapper .form-box li .title {
  width: 25vw;
  font-size: 4vw;
  color: #666;
}

.wrapper .form-box li .content {
  flex: 1;
  display: flex;
  align-items: center;
}

.wrapper .form-box li .content input[type="text"] {
  width: 100%;
  border: none;
  outline: none;
  font-size: 4vw;
  color: #333;
  background-color: transparent;
}

.wrapper .form-box li .content input[type="text"]:disabled {
  color: #999;
  background-color: #f5f5f5;
}

.wrapper .form-box li .content input[type="radio"] {
  margin-right: 2vw;
}

/****************** 保存按钮 ******************/
.wrapper .button-save {
  width: 100%;
  padding: 6vw 4vw;
  box-sizing: border-box;
  margin-top: 4vw;
}

.wrapper .button-save button {
  width: 100%;
  height: 12vw;
  background-color: #0097ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 4.5vw;
  cursor: pointer;
  user-select: none;
}

.wrapper .button-save button:active {
  background-color: #0080e0;
}
</style>
