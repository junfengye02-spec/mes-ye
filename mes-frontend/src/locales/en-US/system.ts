export default {
  title: 'System',
  user: {
    title: 'User Management',
    fields: {
      username: 'Username',
      realName: 'Real Name',
      phone: 'Phone',
      email: 'Email',
      accountType: 'Account Type',
      roles: 'Roles',
      enabled: 'Enabled',
    },
    accountTypes: {
      ADMIN: 'Administrator',
      STAFF: 'Shop-floor Staff',
    },
    actions: {
      resetPassword: 'Reset Password',
      assignRoles: 'Assign Roles',
    },
  },
  role: {
    title: 'Role Management',
    fields: {
      roleCode: 'Role Code',
      roleName: 'Role Name',
      dataScope: 'Data Scope',
    },
    actions: {
      assignMenus: 'Assign Menus',
      assignPermissions: 'Assign Permissions',
    },
  },
  menu: {
    title: 'Menu Management',
    fields: {
      menuName: 'Menu Name',
      path: 'Route Path',
      component: 'Component',
      icon: 'Icon',
      sort: 'Sort',
    },
  },
}
