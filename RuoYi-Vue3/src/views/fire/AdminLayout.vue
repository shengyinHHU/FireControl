<template><Layout class="fire-admin fire-theme" /></template>
<script setup>
import Layout from '@/layout'
</script>
<style lang="scss">
.fire-admin {
  --navbar-bg: #081f34;
  --navbar-text: #bdd1e2;
  --sidebar-bg: #071d32;
  --sidebar-text: #9bb8cc;
  --menu-active-text: #43d0df;
  --menu-hover: #10384f;
  .navbar,
  .tags-view-container,
  .fixed-header,
  .sidebar-logo-container {
    background: #081f34 !important;
    color: #bdd1e2;
  }
  .sidebar-container,
  .scrollbar-wrapper,
  .el-menu,
  .el-menu-item,
  .el-sub-menu__title {
    background: #071d32 !important;
    color: #9bb8cc !important;
  }
  .el-menu-item.is-active {
    color: #43d0df !important;
    background: #10384f !important;
  }
  .el-sub-menu__title:hover,
  .el-menu-item:hover {
    background: #10384f !important;
  }
  .tags-view-container {
    border-bottom-color: #21465d;
    box-shadow: none;
  }
  .tags-view-item {
    color: #9bb8cc !important;
    border-color: #25485f !important;
    background: #0b2942 !important;
  }
  .tags-view-item.active {
    background: #146078 !important;
    color: #d6f4fa !important;
  }
  .app-main {
    background: #06192e;
  }
  .el-breadcrumb__inner,
  .hamburger,
  .right-menu-item {
    color: #a6c4d7 !important;
  }
}
</style>
