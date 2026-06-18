<template>
  <el-container class="layout-container">
    <el-aside :width="collapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="24" color="#fff"><Headset /></el-icon>
        <span v-show="!collapse" class="logo-text">乐器配件管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>总览</template>
        </el-menu-item>
        <el-menu-item index="/accessories">
          <el-icon><Goods /></el-icon>
          <template #title>配件建档</template>
        </el-menu-item>
        <el-menu-item index="/replacements">
          <el-icon><RefreshRight /></el-icon>
          <template #title>更换周期</template>
        </el-menu-item>
        <el-menu-item index="/worn-status">
          <el-icon><Warning /></el-icon>
          <template #title>损耗状态</template>
        </el-menu-item>
        <el-menu-item index="/spec-comparison">
          <el-icon><Grid /></el-icon>
          <template #title>规格对照</template>
        </el-menu-item>
        <el-menu-item index="/groups">
          <el-icon><Folder /></el-icon>
          <template #title>物资分组</template>
        </el-menu-item>
        <el-menu-item index="/sets">
          <el-icon><Box /></el-icon>
          <template #title>套装档案</template>
        </el-menu-item>
        <el-menu-item index="/group-display">
          <el-icon><Grid /></el-icon>
          <template #title>陈列看板</template>
        </el-menu-item>
        <el-menu-item index="/replacement-calendar">
          <el-icon><Calendar /></el-icon>
          <template #title>更换日历</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="toggleCollapse">
            <Fold v-if="!collapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag type="info" effect="plain">个人管理册</el-tag>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Headset, DataAnalysis, Goods, RefreshRight, Warning, Grid, Folder, Calendar, Box } from '@element-plus/icons-vue'

const route = useRoute()
const collapse = ref(false)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

const toggleCollapse = () => {
  collapse.value = !collapse.value
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background: #1f2d3d;
  transition: width 0.28s;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    background: #263445;
    color: #fff;

    .logo-text {
      font-size: 15px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  :deep(.el-menu) {
    border-right: none;
    flex: 1;
  }
}

.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .collapse-btn {
    cursor: pointer;
    color: #606266;
    transition: color 0.2s;

    &:hover {
      color: #409eff;
    }
  }
}

.main {
  padding: 0;
  background: #f5f7fa;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
