/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [require("../../shared/ui/tailwind/preset.js")],
  content: [
    "./captive/web/src/**/*.html",
    "./shared/ui/src/**/*.html",
    "./shared/ui/src/**/*.js",
  ],
  safelist: [],
};

