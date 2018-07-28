# ScaleImageView
**android图片缩放库，支持单击、双击、长按、拖拽、多点触控缩放**
## 使用方法：
将以下内容添加到根目录下的`build.gradle`(**注意**：不是`module:app`下的`build.gradle`）
```
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```
然后将库依赖添加到`module:app`目录下的`build.gradle`
```
dependencies {
    implementation 'com.github.yongjianx:ScaleImageView:v1.0'
}
```


