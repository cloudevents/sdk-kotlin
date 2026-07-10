export default {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "header-max-length": [2, "always", 80],
    "subject-case": [2, "always", "lower-case"],
    "subject-full-stop": [2, "never", "."],
  },
};
