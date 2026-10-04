<template>
  <div class="chat-wrapper">
    <!-- 顶部栏：返回上一页 -->
    <header class="chat-header">
      <i class="fa fa-chevron-left back" @click="goBack"></i>
      <p>AI智能客服</p>
      <span class="online"><i class="fa fa-circle"></i> 在线</span>
    </header>

    <!-- 聊天记录区 -->
    <div class="chat-body" ref="chatBody">
      <div
          v-for="(msg, index) in messages"
          :key="index"
          class="msg"
          :class="msg.role"
      >
        <div class="bubble">{{ msg.content }}</div>
      </div>
      <div v-if="loading" class="msg assistant">
        <div class="bubble typing"><i class="fa fa-ellipsis-h"></i></div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input">
      <input
          v-model="input"
          type="text"
          placeholder="请输入您的问题..."
          @keyup.enter="send"
      />
      <button @click="send" :disabled="loading">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, nextTick } from "vue";
import { useRouter } from "vue-router";

const axios = inject("axios");
const router = useRouter();

const input = ref("");
const loading = ref(false);
const chatBody = ref(null);

// 初始欢迎语（开头词）
const messages = ref([
  {
    role: "assistant",
    content: "您好！我是饿了么智能客服小饿，请问有什么可以帮您？您可以咨询订单、配送、优惠活动等问题哦～",
  },
]);

// 返回上一页
const goBack = () => {
  router.back();
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBody.value) {
      chatBody.value.scrollTop = chatBody.value.scrollHeight;
    }
  });
};

const send = async () => {
  const text = input.value.trim();
  if (!text || loading.value) return;

  messages.value.push({ role: "user", content: text });
  input.value = "";
  loading.value = true;
  scrollToBottom();

  try {
    // 把欢迎语之后、当前问题之前的历史记录传给后端，保持上下文
    const history = messages.value
        .slice(1, -1)
        .map((m) => ({ role: m.role, content: m.content }));

    const response = await axios.post("ChatController/chat", {
      message: text,
      history,
    });
    const data = response.data || {};
    const reply = data.success
        ? data.reply
        : "抱歉，AI客服暂时无法回答，请稍后再试。";
    messages.value.push({ role: "assistant", content: reply });
  } catch (error) {
    console.error("AI客服请求失败", error);
    messages.value.push({
      role: "assistant",
      content: "AI客服连接失败，请检查后端 DeepSeek API Key 配置后重试。",
    });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
};
</script>

<style scoped>
/****************** 总容器 ******************/
.chat-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  box-sizing: border-box;
}

/****************** 顶部栏 ******************/
.chat-header {
  width: 100%;
  height: 12vw;
  background-color: #0097ff;
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 3vw;
  box-sizing: border-box;
  flex-shrink: 0;
}

.chat-header .back {
  font-size: 5vw;
  cursor: pointer;
  margin-right: 2vw;
}

.chat-header p {
  flex: 1;
  font-size: 4.8vw;
  margin: 0;
}

.chat-header .online {
  font-size: 3vw;
  opacity: 0.9;
}

.chat-header .online .fa-circle {
  color: #4cd964;
  font-size: 2.5vw;
  margin-right: 1vw;
}

/****************** 聊天记录区 ******************/
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 3vw;
  box-sizing: border-box;
}

.msg {
  display: flex;
  margin-bottom: 3vw;
}

.msg.user {
  justify-content: flex-end;
}

.msg .bubble {
  max-width: 70%;
  padding: 3vw 4vw;
  border-radius: 2.5vw;
  font-size: 3.8vw;
  line-height: 1.5;
  word-break: break-word;
}

.msg.assistant .bubble {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 0.5vw;
}

.msg.user .bubble {
  background-color: #0097ff;
  color: #fff;
  border-top-right-radius: 0.5vw;
}

.msg.assistant .bubble.typing {
  color: #999;
}

/****************** 输入区 ******************/
.chat-input {
  width: 100%;
  height: 14vw;
  background-color: #fff;
  border-top: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  padding: 0 3vw;
  box-sizing: border-box;
  flex-shrink: 0;
}

.chat-input input {
  flex: 1;
  height: 9vw;
  border: 1px solid #ddd;
  border-radius: 2vw;
  padding: 0 3vw;
  font-size: 3.8vw;
  outline: none;
  box-sizing: border-box;
}

.chat-input button {
  width: 18vw;
  height: 9vw;
  margin-left: 2vw;
  background-color: #0097ff;
  color: #fff;
  border: none;
  border-radius: 2vw;
  font-size: 4vw;
  cursor: pointer;
  user-select: none;
}

.chat-input button:active {
  background-color: #0080e0;
}

.chat-input button:disabled {
  opacity: 0.6;
}
</style>
