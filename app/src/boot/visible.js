import { boot } from "quasar/wrappers";

export default boot(({ app }) => {
  app.directive("visible", function (el, binding) {
    el.style.visibility = !!binding.value ? "visible" : "hidden";
  });
});
