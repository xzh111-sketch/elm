<template>
  <div
      v-if="show"
      class="floating-chat"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @mousedown="onStart"
      @touchstart="onStart"
  >
    <i class="fa fa-comments"></i>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const SIZE = 56; // 悬浮按钮直径（px）

// 底部菜单高度是 14vw，算一个不会被它盖住的位置
const initialBottom = Math.max(120, 0.14 * window.innerWidth + 16);

// 聊天页打开时隐藏悬浮窗，避免遮挡聊天界面
const show = computed(() => route.path !== "/chat");

// 初始位置：右侧垂直居中偏下
const position = ref({
  x: window.innerWidth - SIZE - 12,
  y: window.innerHeight - SIZE - initialBottom,
});

const dragState = ref(null);

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const onStart = (event) => {
  if (event.type === "mousedown" && event.button !== 0) return;
  const clientX = event.touches ? event.touches[0].clientX : event.clientX;
  const clientY = event.touches ? event.touches[0].clientY : event.clientY;
  dragState.value = {
    startX: clientX,
    startY: clientY,
    originX: position.value.x,
    originY: position.value.y,
    moved: false,
  };
  window.addEventListener("mousemove", onMove);
  window.addEventListener("mouseup", onEnd);
  window.addEventListener("touchmove", onMove, { passive: false });
  window.addEventListener("touchend", onEnd);
};

const onMove = (event) => {
  if (!dragState.value) return;
  event.preventDefault();
  const clientX = event.touches ? event.touches[0].clientX : event.clientX;
  const clientY = event.touches ? event.touches[0].clientY : event.clientY;
  const dx = clientX - dragState.value.startX;
  const dy = clientY - dragState.value.startY;
  if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
    dragState.value.moved = true;
  }
  position.value.x = clamp(dragState.value.originX + dx, 0, window.innerWidth - SIZE);
  position.value.y = clamp(dragState.value.originY + dy, 0, window.innerHeight - SIZE);
};

const onEnd = () => {
  if (!dragState.value) return;
  // 松手后吸附到最近的侧边
  const leftDist = position.value.x;
  const rightDist = window.innerWidth - SIZE - position.value.x;
  position.value.x = leftDist <= rightDist ? 0 : window.innerWidth - SIZE;
  position.value.y = clamp(position.value.y, 0, window.innerHeight - SIZE);

  const wasClick = !dragState.value.moved;
  dragState.value = null;

  window.removeEventListener("mousemove", onMove);
  window.removeEventListener("mouseup", onEnd);
  window.removeEventListener("touchmove", onMove);
  window.removeEventListener("touchend", onEnd);

  // 没有拖动距离则视为点击，进入聊天页
  if (wasClick) {
    router.push("/chat");
  }
};

onUnmounted(() => {
  window.removeEventListener("mousemove", onMove);
  window.removeEventListener("mouseup", onEnd);
  window.removeEventListener("touchmove", onMove);
  window.removeEventListener("touchend", onEnd);
});
</script>

<style scoped>
.floating-chat {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: #0097ff;
  color: #fff;
  font-size: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  z-index: 10000; /* 必须高于底部菜单的 9999，否则会被盖住点不到 */
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  user-select: none;
  touch-action: none;
}

.floating-chat:active {
  background-color: #0080e0;
}
</style>
