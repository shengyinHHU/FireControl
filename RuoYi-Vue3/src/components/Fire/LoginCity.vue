<template>
  <svg class="login-city" viewBox="0 0 860 470" fill="none" aria-hidden="true" focusable="false">
    <defs>
      <linearGradient id="login-ground" x1="430" y1="155" x2="430" y2="470" gradientUnits="userSpaceOnUse"><stop stop-color="#1d7190" stop-opacity=".04" /><stop offset=".55" stop-color="#22c6e8" stop-opacity=".11" /><stop offset="1" stop-color="#22c6e8" stop-opacity="0" /></linearGradient>
      <linearGradient id="login-facade" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#1b5b77" stop-opacity=".85" /><stop offset="1" stop-color="#0c2d46" stop-opacity=".7" /></linearGradient>
      <linearGradient id="login-light" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#8fe8f5" /><stop offset="1" stop-color="#22c6e8" stop-opacity=".2" /></linearGradient>
      <radialGradient id="login-beacon"><stop stop-color="#63e7f8" stop-opacity=".25" /><stop offset="1" stop-color="#22c6e8" stop-opacity="0" /></radialGradient>
      <linearGradient id="login-line" x1="150" y1="280" x2="720" y2="280" gradientUnits="userSpaceOnUse"><stop stop-color="#22c6e8" stop-opacity="0" /><stop offset=".5" stop-color="#75ddeb" stop-opacity=".8" /><stop offset="1" stop-color="#22c6e8" stop-opacity="0" /></linearGradient>
    </defs>
    <path d="M40 262 419 59 829 284 452 487Z" fill="url(#login-ground)" />
    <g stroke="#55acc4" stroke-opacity=".1" stroke-width=".8">
      <path v-for="i in 11" :key="`u${i}`" :d="`M${40 + i * 34} ${262 - i * 18.2} l410 225`" />
      <path v-for="i in 11" :key="`v${i}`" :d="`M${40 + i * 37.3} ${262 + i * 20.45} l379 -203`" />
    </g>
    <path d="m117 281 272-145 346 190-272 144Z" stroke="url(#login-line)" stroke-width="1.2" />
    <path d="m178 315 118-63 137 75 175-94 100 55" stroke="#2c839e" stroke-width="2" />
    <path d="m206 331 90-48 137 75 175-94 70 38" stroke="#69d7e8" stroke-opacity=".5" stroke-width="1" stroke-dasharray="4 8" />
    <ellipse cx="429" cy="302" rx="195" ry="107" fill="url(#login-beacon)" />
    <g v-for="building in buildings" :key="building.x" :opacity="building.opacity || 1" stroke-linejoin="round">
      <path :d="front(building)" fill="url(#login-facade)" stroke="#4391ab" stroke-width="1" />
      <path :d="side(building)" fill="#0b2b43" stroke="#30758e" stroke-width="1" />
      <path :d="roof(building)" fill="#163c54" stroke="#65c1d5" stroke-width="1.2" />
      <path v-for="floor in Math.floor(building.h / 14)" :key="floor" :d="`M${building.x + 7} ${building.y - floor * 14} l${building.w - 14} ${-(building.w - 14) / 2}`" stroke="#5fd6e8" :stroke-opacity="floor % 3 === 0 ? '.65' : '.24'" stroke-width="1.5" />
      <path :d="`M${building.x + building.w / 2} ${building.y - building.w / 4} v${-building.h}`" stroke="#4391ab" stroke-opacity=".55" />
    </g>
    <g class="login-signal">
      <path d="M430 245V105" stroke="url(#login-light)" stroke-width="1.3" stroke-dasharray="3 6" />
      <ellipse cx="430" cy="250" rx="43" ry="23" stroke="#77e0ee" stroke-opacity=".6" />
      <ellipse cx="430" cy="250" rx="62" ry="33" stroke="#77e0ee" stroke-opacity=".15" />
      <path d="m430 62 28 15v30c0 21-28 38-28 38s-28-17-28-38V77Z" fill="#0b2b43" stroke="#8ce4ee" stroke-width="1.5" />
      <path d="M430 79c2 10 11 14 9 24 5-3 8-2 8 5 0 12-8 20-17 20s-17-7-17-17c0-9 8-13 8-23 5 3 6 7 6 11 5-6 4-13 3-20Z" fill="#f3ae7d" />
      <path d="M430 108c4 4 6 7 5 11-2 5-8 5-10 1-2-4 2-7 5-12Z" fill="#fff1d5" />
    </g>
    <g stroke="#79d6e7" stroke-width="1.1">
      <path d="m224 226-54-30H98" stroke-opacity=".6" /><circle cx="224" cy="226" r="3" fill="#75ddeb" />
      <path d="m617 299 63-34h81" stroke-opacity=".6" /><circle cx="617" cy="299" r="3" fill="#75ddeb" />
    </g>
    <g fill="#9fc8d8" font-size="12" font-family="Microsoft YaHei, sans-serif" letter-spacing="2">
      <text x="98" y="185">物联感知</text><text x="688" y="254">视频联动</text>
    </g>
  </svg>
</template>
<script setup>
const buildings = [
  { x: 345, y: 220, w: 43, d: 31, h: 64, opacity: .6 },
  { x: 533, y: 256, w: 56, d: 40, h: 98, opacity: .75 },
  { x: 635, y: 314, w: 45, d: 40, h: 60, opacity: .8 },
  { x: 260, y: 285, w: 57, d: 42, h: 92 },
  { x: 391, y: 324, w: 76, d: 53, h: 137 },
  { x: 506, y: 379, w: 67, d: 45, h: 91 },
  { x: 327, y: 395, w: 51, d: 37, h: 60, opacity: .85 },
]
const front = b => `M${b.x} ${b.y} l${b.w} ${-b.w / 2} v${-b.h} l${-b.w} ${b.w / 2}Z`
const side = b => `M${b.x} ${b.y} l${-b.d} ${-b.d / 2} v${-b.h} l${b.d} ${b.d / 2}Z`
const roof = b => `M${b.x} ${b.y - b.h} l${b.w} ${-b.w / 2} l${-b.d} ${-b.d / 2} l${-b.w} ${b.w / 2}Z`
</script>
