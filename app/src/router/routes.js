const routes = [
  {
    path: "/",
    component: () => import("layouts/MainLayout.vue"),
  },
  {
    path: "/example",
    component: () => import("layouts/ExampleLayout.vue"),
    children: [{ path: "", component: () => import("pages/IndexPage.vue") }],
  },

  {
    path: "/:catchAll(.*)*",
    component: () => import("pages/ErrorNotFound.vue"),
  },
];

export default routes;
