export default {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "header-max-length": [2, "always", 80],
    "subject-full-stop": [2, "never", "."],
    // subject-case inherits @commitlint/config-conventional's default
    // ([2, "never", ["sentence-case","start-case","pascal-case","upper-case"]]):
    // the description must not be Sentence/Title/ALL-CAPS, but proper nouns and
    // type names such as CloudEvent or SpecVersion may keep their casing.
  },
};
