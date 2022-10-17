<div align="center">
  <img src=".github/komako.jpg" width = "300" height = "300" alt="Komako"><br>
  <h1>copymanga 拷贝漫画</h1>
  拷贝漫画的第三方APP，优化阅读/下载体验<br><br>

  [<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/top.fumiama.copymanga)

</div>

<div align=center> <a href="#"> <img src="http://cmoe.azurewebsites.net/cmoe?name=copymanga&theme=gb" /> </a> </div>

## 提示
漫画数据均来源于拷贝漫画官方，作者不对其中呈现的任何内容负责。

## 说明
1. 下载文件为`webp`格式图片(新版api已改成`jpg`)，按章节打包为`zip`，可使用本应用或其他漫画阅读应用打开。
2. 若想查看下载的漫画是否有错误，可以长按该漫画目录执行查错。
3. 下载文件位于`./Android/data/top.fumiama.copymanga/files`目录。

## 1. 网页版(1.x)
网页版是指基于官方的`HTML5`手机版本制作的网页客户端，由于官方的网页功能比较完善，再加上套壳体积较小，所以适合喜欢精简版的用户使用。

但是也正因为是浏览器套壳，因此其性能与美观程度可能不足。

网页版位于分支`web`，同时在`release`中具有版本号`1.x`。

如果想升级到新版，只需要覆盖安装即可，下载的漫画不会丢失。

## 2. 新版(2.0+)
1. 官方在某段时间关闭`H5`后（现已重新打开），从`2.0`版本开始，本应用进行了全新升级。
2. 新版使用`Material Design`配合官方`APP`的`API`。
3. 本应用默认使用海外线路。

### 功能
1. 浏览主页、分类、排行、我的下载、标签、作者。
2. 查看、搜索漫画并直接阅读；记录漫画与章节的阅读进度。
3. 下载漫画。但是由于不可抗力，下载速度较慢且容易出错，这绝对不是优化的原因，绝对不是。
4. 阅读下载的漫画。
5. 检查更新。

### 未实现功能
未在上表列出的官方`APP`的其他功能。

### 预览
#### 浅色模式

<table>
	<tr>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217391-7f617392-4ad4-47cf-b903-fa445db6fcfc.png"></td>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217403-0d4822e0-5c8d-4be5-b300-e9cc9a9f09d6.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217414-198fd7d2-ed80-4c0e-a40c-c83ac9ff091d.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217423-2d85a8d3-1213-4bd0-84a5-0a70234edc95.png"></td>
	</tr>
    <tr>
		<td align="center">主页</td>
		<td align="center">详情</td>
        <td align="center">阅读</td>
        <td align="center">标签</td>
	</tr>
</table>
<table>
	<tr>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217443-a99a93e6-7d45-4801-9138-c3fc62064f5c.png"></td>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217462-3f25eee2-d356-420a-b129-754725201f36.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217475-3f4b1c5b-d885-4338-9312-26330a1fabd5.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217483-5fefa526-649b-4f7c-812e-81c4b1592b35.png"></td>
	</tr>
    <tr>
		<td align="center">分类</td>
		<td align="center">下载</td>
        <td align="center">正在下载</td>
        <td align="center">抽屉</td>
	</tr>
</table>

#### 深色模式

<table>
	<tr>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217254-5fc9b56b-2800-4cb8-bbeb-5020e2b0387d.png"></td>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217300-3bdb4209-9d2e-41d6-9418-7defda27667a.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217310-c245eddc-1698-454d-96ad-456b81f469cb.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217327-7f44cd96-aaee-4e23-b4df-eed4e61b289c.png"></td>
	</tr>
    <tr>
		<td align="center">主页</td>
		<td align="center">详情</td>
        <td align="center">阅读</td>
        <td align="center">标签</td>
	</tr>
</table>
<table>
	<tr>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217344-2e8024f5-cbb1-48a4-8eff-b834fc6c2326.png"></td>
		<td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217365-be6278f8-684c-44e8-be81-f8a14ced9ac0.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217372-7ca3a1be-ebd9-4a9c-8371-666f91c415db.png"></td>
        <td align="center"><img src="https://user-images.githubusercontent.com/41315874/196217382-95695b78-b435-4b89-a0c3-8c9a7a7ed237.png"></td>
	</tr>
    <tr>
		<td align="center">分类</td>
		<td align="center">下载</td>
        <td align="center">正在下载</td>
        <td align="center">抽屉</td>
	</tr>
</table>
