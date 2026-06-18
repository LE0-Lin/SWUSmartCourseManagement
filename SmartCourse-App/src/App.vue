<template>
  <div id="app">
    <router-view :key="$route.path" />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style scoped lang="scss">
#app {
  height: 100%;
}
</style>
