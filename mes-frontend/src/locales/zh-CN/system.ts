export default {
  title: '系统管理',
  user: {
    title: '用户管理',
    fields: {
      username: '用户名',
      realName: '姓名',
      phone: '手机号',
      email: '邮箱',
      accountType: '账号类型',
      roles: '角色',
      enabled: '启用状态',
    },
    accountTypes: {
      ADMIN: '管理员',
      STAFF: '现场员工',
    },
    actions: {
      resetPassword: '重置密码',
      assignRoles: '分配角色',
    },
  },
  role: {
    title: '角色管理',
    fields: {
      roleCode: '角色编码',
      roleName: '角色名称',
      dataScope: '数据范围',
    },
    actions: {
      assignMenus: '分配菜单',
      assignPermissions: '分配权限',
    },
  },
  menu: {
    title: '菜单管理',
    fields: {
      menuName: '菜单名称',
      path: '路由',
      component: '组件',
      icon: '图标',
      sort: '排序',
    },
  },
}
