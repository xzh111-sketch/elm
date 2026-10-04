<template>
  <div class="wrapper">
    <!-- header部分 -->
    <header>
      <p>购物车</p>
    </header>

    <!-- 购物车列表部分 -->
    <div v-if="cartGroupedByBusiness.length > 0" class="cart-list">
      <div 
        v-for="group in cartGroupedByBusiness" 
        :key="group.businessId" 
        class="cart-business-group"
      >
        <!-- 商家信息 -->
        <div class="business-header">
          <h3>{{ group.business.businessName }}</h3>
        </div>

        <!-- 该商家的商品列表 -->
        <ul class="cart-items">
          <li v-for="item in group.items" :key="item.cartId" class="cart-item">
            <div class="cart-item-left">
              <img :src="item.food.foodImg" alt="" />
              <div class="cart-item-info">
                <p class="food-name">{{ item.food.foodName }}</p>
                <p class="food-price">&#165;{{ item.food.foodPrice }}</p>
              </div>
            </div>
            <div class="cart-item-right">
              <p class="item-total">&#165;{{ (item.food.foodPrice * item.quantity).toFixed(2) }}</p>
              <div class="quantity-control">
                <button class="quantity-btn decrease-btn" @click="decreaseQuantity(item)" :disabled="item.quantity <= 1">-</button>
                <span class="quantity-value">{{ item.quantity }}</span>
                <button class="quantity-btn increase-btn" @click="increaseQuantity(item)">+</button>
              </div>
              <div class="delete-btn" @click="removeCartItem(item)">删除</div>
            </div>
          </li>
        </ul>

        <!-- 该商家的合计信息 -->
        <div class="business-summary">
          <div class="summary-info">
            <p>商品小计：&#165;{{ group.subtotal.toFixed(2) }}</p>
            <p>配送费：&#165;{{ group.business.deliveryPrice.toFixed(2) }}</p>
            <p class="total-price">合计：&#165;{{ group.total.toFixed(2) }}</p>
          </div>
          <div class="to-payment-btn" @click="toOrders(group.businessId)">
            去结算
          </div>
        </div>
      </div>
    </div>

    <!-- 购物车为空时的提示 -->
    <div v-if="cartGroupedByBusiness.length === 0 && initFinished" class="empty-cart">
      <p>购物车是空的</p>
      <p>快去选购商品吧~</p>
    </div>

    <!-- 底部菜单部分 -->
    <Footer></Footer>
  </div>
</template>

<script setup>
import Footer from "@/components/Footer.vue";
import qs from "qs";
import { ref, inject, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { getSessionStorage } from "@/common";

const axios = inject("axios");
const router = useRouter();
const user = getSessionStorage("user");
const cartList = ref([]);
const initFinished = ref(false);

// 校验用户是否登录
const checkUserLogin = () => {
  if (!user || !user.userId) {
    alert("请先登录！");
    router.push("/login");
    return false;
  }
  return true;
};

// 按商家分组购物车数据
const cartGroupedByBusiness = computed(() => {
  const groups = {};
  
  cartList.value.forEach(cartItem => {
    const businessId = cartItem.businessId;
    
    if (!groups[businessId]) {
      groups[businessId] = {
        businessId: businessId,
        business: cartItem.business,
        items: []
      };
    }
    
    groups[businessId].items.push(cartItem);
  });

  // 计算每个商家的总价
  return Object.values(groups).map(group => {
    const subtotal = group.items.reduce((sum, item) => {
      return sum + (item.food.foodPrice * item.quantity);
    }, 0);
    
    group.subtotal = subtotal;
    group.total = subtotal + (group.business.deliveryPrice || 0);
    
    return group;
  });
});

// 初始化：加载购物车数据
const init = async () => {
  if (!checkUserLogin()) return;

  try {
    // 调用后端接口：查询所有购物车
    const response = await axios.post(
      "CartController/listCart",
      qs.stringify({
        userId: user.userId
      })
    );

    cartList.value = response.data || [];
  } catch (error) {
    console.error("加载购物车失败：", error);
    alert("购物车数据加载失败，请稍后重试！");
  } finally {
    initFinished.value = true;
  }
};

// 增加商品数量
const increaseQuantity = async (item) => {
  if (!checkUserLogin()) return;

  const newQuantity = item.quantity + 1;
  
  try {
    const response = await axios.post(
      "CartController/updateCart",
      qs.stringify({
        foodId: item.foodId,
        businessId: item.businessId,
        userId: user.userId,
        quantity: newQuantity
      })
    );

    if (response.data > 0) {
      // 更新本地数据
      item.quantity = newQuantity;
    } else {
      alert("更新数量失败，请稍后重试！");
    }
  } catch (error) {
    console.error(error);
  }
};

// 减少商品数量
const decreaseQuantity = async (item) => {
  if (!checkUserLogin()) return;

  if (item.quantity <= 1) {
    return;
  }

  const newQuantity = item.quantity - 1;
  
  try {
    const response = await axios.post(
      "CartController/updateCart",
      qs.stringify({
        foodId: item.foodId,
        businessId: item.businessId,
        userId: user.userId,
        quantity: newQuantity
      })
    );

    if (response.data > 0) {
      // 更新本地数据
      item.quantity = newQuantity;
    } else {
      alert("更新数量失败，请稍后重试！");
    }
  } catch (error) {
    console.error(error);
  }
};

// 删除购物车商品
const removeCartItem = async (item) => {
  if (!confirm(`确定要删除 ${item.food.foodName} 吗？`)) {
    return;
  }

  if (!checkUserLogin()) return;

  try {
    const response = await axios.post(
      "CartController/removeCart",
      qs.stringify({
        cartId: item.cartId,
        userId: user.userId
      })
    );

    if (response.data > 0) {
      // 重新加载购物车数据
      await init();
    }
  } catch (error) {
    console.error(error);
  }
};

// 跳转到确认订单页面
const toOrders = (businessId) => {
  router.push({
    path: "/orders",
    query: {
      businessId: businessId
    }
  });
};

onMounted(() => {
  init();
});
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
  width: 100%;
  height: 100%;
  padding-bottom: 14vw; /* 给底部菜单留空间 */
  box-sizing: border-box;
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

/****************** 购物车列表部分 ******************/
.wrapper .cart-list {
  margin-top: 12vw;
}

.wrapper .cart-business-group {
  width: 100%;
  margin-bottom: 4vw;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}

.wrapper .business-header {
  width: 100%;
  box-sizing: border-box;
  padding: 3vw 4vw;
  background-color: #f8f8f8;
  border-bottom: 1px solid #eee;
}

.wrapper .business-header h3 {
  font-size: 4.5vw;
  color: #333;
  font-weight: 600;
  margin: 0;
}

.wrapper .cart-items {
  width: 100%;
  margin: 0;
  padding: 0;
  list-style: none;
}

.wrapper .cart-item {
  width: 100%;
  box-sizing: border-box;
  padding: 3vw 4vw;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrapper .cart-item-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.wrapper .cart-item-left img {
  width: 12vw;
  height: 12vw;
  border-radius: 2px;
  margin-right: 3vw;
}

.wrapper .cart-item-info {
  flex: 1;
}

.wrapper .cart-item-info .food-name {
  font-size: 4vw;
  color: #333;
  margin: 0 0 1vw 0;
}

.wrapper .cart-item-info .food-price {
  font-size: 3.5vw;
  color: #999;
  margin: 0;
}

.wrapper .cart-item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.wrapper .cart-item-right .item-total {
  font-size: 4vw;
  color: #f90;
  font-weight: 600;
  margin: 0 0 2vw 0;
}

.wrapper .cart-item-right .quantity-control {
  display: flex;
  align-items: center;
  margin-bottom: 2vw;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.wrapper .cart-item-right .quantity-btn {
  width: 8vw;
  height: 6vw;
  border: none;
  background-color: #f5f5f5;
  color: #333;
  font-size: 4vw;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
}

.wrapper .cart-item-right .quantity-btn:active {
  background-color: #e0e0e0;
}

.wrapper .cart-item-right .quantity-btn:disabled {
  background-color: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}

.wrapper .cart-item-right .quantity-btn:disabled:active {
  background-color: #f5f5f5;
}

.wrapper .cart-item-right .quantity-value {
  min-width: 8vw;
  height: 6vw;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 3.5vw;
  color: #333;
  background-color: #fff;
  border-left: 1px solid #ddd;
  border-right: 1px solid #ddd;
  padding: 0 2vw;
}

.wrapper .cart-item-right .delete-btn {
  font-size: 3.5vw;
  color: #ff4444;
  padding: 1.5vw 3vw;
  border: 1px solid #ff4444;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  text-align: center;
}

.wrapper .cart-item-right .delete-btn:active {
  background-color: #ff4444;
  color: #fff;
}

.wrapper .business-summary {
  width: 100%;
  box-sizing: border-box;
  padding: 3vw 4vw;
  background-color: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrapper .summary-info {
  flex: 1;
}

.wrapper .summary-info p {
  font-size: 3.5vw;
  color: #666;
  margin: 1vw 0;
}

.wrapper .summary-info .total-price {
  font-size: 4.5vw;
  color: #f90;
  font-weight: 600;
  margin-top: 2vw;
}

.wrapper .to-payment-btn {
  background-color: #0097ff;
  color: #fff;
  font-size: 4vw;
  padding: 2.5vw 5vw;
  border-radius: 4px;
  user-select: none;
  cursor: pointer;
  font-weight: 600;
}

.wrapper .to-payment-btn:active {
  background-color: #0080e0;
}

/****************** 空购物车提示 ******************/
.wrapper .empty-cart {
  margin-top: 12vw;
  padding: 10vw 4vw;
  text-align: center;
  color: #999;
}

.wrapper .empty-cart p {
  font-size: 4vw;
  margin: 2vw 0;
}
</style>

