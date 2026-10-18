<template>
  <g class="demo-cartography" aria-hidden="true" pointer-events="none">
    <rect x="-800" y="-800" width="2600" height="2400" fill="#020711" />
    <g fill="none" stroke="#102334" stroke-width="2.1">
      <path v-for="(d,i) in streets" :key="i" :d="d" />
    </g>
    <g fill="#07152b" stroke="#10253c" stroke-width="1">
      <path d="M655 10l50 18 25 60-25 48-39-14-17-53Z" />
      <path d="M790 390l49-29 32 32-20 59-55 26-22-35Z" />
      <path d="M392 590l48-15 42 31-11 47-54 23-33-37Z" />
      <path d="M860 90l50-20 29 23-7 42-41 15-35-28Z" />
    </g>
    <g fill="none" stroke="#17384a" stroke-width="5" opacity=".8">
      <path v-for="(d,i) in arteries" :key="i" :d="d" />
    </g>
    <path d="M120 -150 C320 -5 407 56 394 175 S297 340 274 451 S139 631 120 792 L90 1000 235 1000 252 795 C266 650 381 569 400 457 S445 286 478 184 S501 -4 326 -150Z" fill="#08172c" stroke="#14263b" stroke-width="2" />
    <path d="M339 199 C405 174 459 162 503 126 L520 169 C467 202 425 224 359 243Z" fill="#030a17" />
    <g fill="none" stroke="#224156" stroke-width="5">
      <path d="M242 310L438 350M165 578L326 626M333 88L502 142" />
    </g>
    <g fill="#0c2932" opacity=".65">
      <path d="M98 196l50-28 56 24-13 62-66 10-34-30Z" />
      <path d="M527 642l34-23 54 16 22 40-35 32-64-18Z" />
      <path d="M718 704l34-24 46 20 12 41-50 26-47-19Z" />
    </g>
    <g v-if="showPoi" class="poi-labels" fill="#16a0bf" font-size="16">
      <text v-for="p in labels" :key="p[2]" :x="p[0]" :y="p[1]">{{ p[2] }}</text>
    </g>
    <g fill="#45667c" font-size="11" letter-spacing="3">
      <text x="490" y="245" transform="rotate(-24 490 245)">葛关路</text>
      <text x="620" y="528" transform="rotate(-35 620 528)">新华路</text>
      <text x="385" y="710" transform="rotate(-30 385 710)">江北大道</text>
      <text x="336" y="385" transform="rotate(-65 336 385)" fill="#35628a" font-size="20" letter-spacing="14">长江</text>
    </g>
  </g>
</template>
<script setup>
defineProps({ showPoi: Boolean })
// Deterministic vector mock: illustrative roads and labels, not surveyed GIS data.
let seed = 27
const random = () => { seed = (seed * 16807) % 2147483647; return seed / 2147483647 }
const streets = []
for (let band = 0; band < 1; band++) {
  for (let row = -5; row < 37; row++) {
    const y = row * 29 + random() * 10
    let d = `M -300 ${y}`
    for (let col = 0; col < 12; col++) {
      const x = -180 + col * 130
      const bend = Math.sin(col * .8 + band) * 35
      d += ` L ${x} ${y + bend + col * (band === 1 ? -12 : 9) + random() * 14}`
    }
    streets.push(d)
  }
}
for (let col = -5; col < 44; col++) {
  let d = `M ${col * 31} -250`
  for (let row = 0; row < 14; row++) d += ` L ${col * 31 + Math.sin(row * .65 + col * .15) * 48 + row * 6} ${-160 + row * 90}`
  streets.push(d)
}
const arteries = [
  'M-100 180 Q180 120 370 215 T1100 80',
  'M-100 460 Q160 415 350 480 T1100 310',
  'M-100 760 Q220 710 460 590 T1100 540',
  'M520 -100 Q630 170 585 350 T470 950',
  'M920 -100 Q790 190 821 480 T1050 950',
  'M-100 590 Q220 400 550 190 T1030 -80',
  'M200 950 Q450 750 700 560 T1100 260',
]
const labels = [[65,125,'南京江北新区'],[540,95,'大厂街道'],[710,175,'扬子石化'],[75,300,'长芦街道'],[510,170,'葛塘街道'],[70,453,'滨江湿地公园'],[525,300,'大厂高级中学'],[790,290,'扬子医院'],[480,425,'新华广场'],[750,560,'南化公司'],[90,690,'江北滨江绿道'],[495,730,'沿江街道'],[735,660,'平顶山公园'],[650,785,'江北新区产业园']]
</script>
<style scoped>
.poi-labels { paint-order:stroke; stroke:#020711; stroke-width:3px; stroke-linejoin:round; }
</style>
