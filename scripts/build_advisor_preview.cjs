// Compile the actual Vue component for a local, fixture-backed preview.
const fs = require('fs')
const path = require('path')
const root = path.resolve(__dirname, '..')
const deps = path.join(root, 'SmartCourse-App', 'node_modules')
const compiler = require(path.join(deps, 'vue-template-compiler'))
const sass = require(path.join(deps, 'sass'))
const source = fs.readFileSync(path.join(root, 'SmartCourse-App/src/views/advisor/index.vue'), 'utf8')
const component = compiler.parseComponent(source)
const template = compiler.compile(component.template.content)
if (template.errors.length) throw new Error(template.errors.join('\n'))
const script = component.script.content
  .replace(/^import \{ getSmartAdvisorDashboard, diagnoseCourse \} from '@\/api\/content'\s*$/m, '')
  .replace('export default', 'const Advisor =')
if (script.includes('export default') || /^import /m.test(script)) throw new Error('Unexpected component imports')
const css = sass.renderSync({ data: component.styles.map(item => item.content).join('\n') }).css.toString()
const target = path.join(root, 'build/advisor-preview')
fs.mkdirSync(target, { recursive: true })
fs.writeFileSync(path.join(target, 'index.html'), `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>SmartCourse · Advisor regression demo</title>
<link rel="stylesheet" href="/assets/element.css">
<style>body{margin:0;font-family:system-ui,'Microsoft YaHei',sans-serif;background:#f5f7fb}.demo-bar{padding:16px 4%;background:#102d34;color:white;display:flex;gap:18px;align-items:center;flex-wrap:wrap}.demo-bar a{color:#a7f3d0}.demo-bar small{opacity:.8}.planning-report{margin-bottom:18px}${css}</style></head>
<body><nav class="demo-bar"><strong>SmartCourse · 可复现演示</strong>
<a href="/?scenario=full-catalog">第 13 门精确补齐</a><a href="/?scenario=missing-candidate">候选课表缺失</a><a href="/?scenario=missing-selected">已选课表缺失</a><a href="/?scenario=search-limit">搜索上限</a>
<small>真实 Vue 组件 · JDBC 集成测试生成的合成数据 · 只读，不执行选课</small></nav><div id="app"><advisor /></div>
<script src="/assets/vue.js"></script><script src="/assets/element.js"></script><script>
const scenario = new URLSearchParams(location.search).get('scenario') || 'full-catalog';
const getSmartAdvisorDashboard = () => fetch('/fixture/' + encodeURIComponent(scenario)).then(r => { if (!r.ok) throw new Error('Run mvn test to create fixtures'); return r.json(); }).then(data => ({data}));
const diagnoseCourse = () => Promise.resolve({data:{selectable:false,message:'这是只读回归演示；完整诊断接口需要登录运行中的系统。'}});
${script}
Advisor.render = new Function(${JSON.stringify(template.render)});
Advisor.staticRenderFns = ${JSON.stringify(template.staticRenderFns)}.map(body => new Function(body));
Vue.use(ELEMENT); new Vue({el:'#app',components:{Advisor}});
</script></body></html>`)
process.stdout.write('Built actual advisor component preview.\n')
