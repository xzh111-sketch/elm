import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import 'font-awesome/css/font-awesome.min.css'

router.beforeEach(function(to,from,next){
    let user = sessionStorage.getItem('user');
    //除了登录、注册、首页、商家列表、商家信息之外，都需要判断是否登录
    if(!(to.path=='/'||to.path=='/index'||to.path=='/businessList'||to.path=='/businessInfo'||to.path=='/login'||to.path=='/register'||to.path=='/chat'||to.path=='/pay-result')){
        if(user==null){
            router.push('/login');
            //location.reload();  //Vue3.0中不需要此代码
        }
    }
    next();
});

createApp(App).use(router).mount('#app')
