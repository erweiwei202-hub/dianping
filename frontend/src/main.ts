import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import HomeView from './views/HomeView.vue'
import AdminView from './views/AdminView.vue'
import AccountView from './views/AccountView.vue'
import './style.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/account', name: 'account', component: AccountView },
    { path: '/admin', name: 'admin', component: AdminView },
  ],
  scrollBehavior: () => ({ top: 0, behavior: 'smooth' }),
})

createApp(App).use(createPinia()).use(router).mount('#app')
