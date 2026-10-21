import '@fontsource-variable/geist'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { i18n } from './i18n'
import { router } from './router'
import './styles.css'

const app = createApp(App)
app.use(createPinia())
app.use(i18n)
app.use(router)

window.addEventListener('kb:unauthorized', () => {
  const current = router.currentRoute.value
  if (current.name === 'login') {
    return
  }
  void router.push({
    name: 'login',
    query: { redirect: current.fullPath },
  })
})

app.mount('#app')

