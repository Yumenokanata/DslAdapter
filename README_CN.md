# DslAdapter
[![](https://jitpack.io/v/Yumenokanata/DslAdapter.svg)](https://jitpack.io/#Yumenokanata/DslAdapter)

一个用于Android RecyclerView的Adapter构建工具, DSL语法, **面向组合子设计**. 专注**类型安全**, 所有代码采用Kotlin编写.

* 类型安全的Adapter
* 使用异构列表HList实现完全的静态类型
* DSL构建器
* DSL更新器
* 面向组合子设计, 极其灵活、便于扩展
* 函数范式设计, 数据和算法分离, 副作用隔离

---

## 相关文章

1. [DslAdapter开发简介](https://segmentfault.com/a/1190000018754660)
2. [异构列表(DslAdapter开发日志)](https://segmentfault.com/a/1190000017497007)

---

## 配置方法

Step 1. 在项目根目录的 build.gradle 文件结尾添加仓库:
```groovy
allprojects {
	repositories {
        jcenter()
		maven { url "https://jitpack.io" }
	}
}
```

Step 2. 在需要使用的module的gradle文件中添加依赖:

从 Ver2.0:

```groovy
dependencies {
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter:x.y.z'
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-rx2:x.y.z' // Beta, optional
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-position:x.y.z' // optional
}
```

Ver2.0 之前:

```groovy
dependencies {
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter:x.y.z'
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-updater:x.y.z'
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-rx2:x.y.z' // Beta, optional
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-position:x.y.z' // optional
}
```

[示例](https://github.com/Yumenokanata/DslAdapter/blob/master/sample/src/main/java/indi/yume/tools/sample/MainActivity.kt)


---

## 概览

现在你可以使用此工具构建复杂的RecyclerView的Adapter了(**Adapter的泛型中包含整个复杂结构的类型信息**):

```kotlin
val adapter = RendererAdapter.multipleBuild()
        .add(layout<Unit>(R.layout.list_header))
        .add(none<List<Option<ItemModel>>>(),
                optionRenderer(
                        noneItemRenderer = LayoutRenderer.dataBindingItem<Unit, ItemLayoutBinding>(
                                count = 5,
                                layout = R.layout.item_layout,
                                bindBinding = { ItemLayoutBinding.bind(it) },
                                binder = { bind, item, _ ->
                                    bind.content = "this is empty item"
                                },
                                recycleFun = { it.model = null; it.content = null; it.click = null }),
                        itemRenderer = LayoutRenderer.dataBindingItem<Option<ItemModel>, ItemLayoutBinding>(
                                count = 5,
                                layout = R.layout.item_layout,
                                bindBinding = { ItemLayoutBinding.bind(it) },
                                binder = { bind, item, _ ->
                                    bind.content = "this is some item"
                                },
                                recycleFun = { it.model = null; it.content = null; it.click = null })
                                .forList()
                ))
        .add(provideData(index).let { HListK.singleId(it).putF(it) },
                ComposeRenderer.startBuild
                        .add(LayoutRenderer<ItemModel>(layout = R.layout.simple_item,
                                stableIdForItem = { item, index -> item.id },
                                binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = itemModel.title },
                                recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                                .forList({ i, index -> index }))
                        .add(databindingOf<ItemModel>(R.layout.item_layout)
                                .onRecycle(CLEAR_ALL)
                                .itemId(BR.model)
                                .itemId(BR.content, { m -> m.content + "xxxx" })
                                .stableIdForItem { it.id }
                                .forList())
                        .build())
        .add(provideData(index),
                LayoutRenderer<ItemModel>(
                        count = 2,
                        layout = R.layout.simple_item,
                        stableIdForItem = { item, index -> item.id },
                        binder = { view, itemModel, index -> view.findViewById<TextView>(R.id.simple_text_view).text = itemModel.title },
                        recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
                        .forList({ i, index -> index }))
        .add(provideData(index), renderer)
        .add(DateFormat.getInstance().format(Date()),
                databindingOf<String>(R.layout.list_footer)
                        .itemId(BR.text)
                        .forItem())
        .build()
```

以上代码构建出的Adapter可图示为:

```
|--LayoutRenderer  header
|
|--SealedItemRenderer
|    |--none -> LayoutRenderer placeholder count 5
|    |
|    |--some -> ListRenderer
|                 |--DataBindingRenderer 1
|                 |--DataBindingRenderer 2
|                 |--...
|
|--ListRenderer
|    |--LayoutRenderer simple item1
|    |--LayoutRenderer simple item2
|    |--...
|
|--ListRenderer
|    |--DataBindingRenderer item with content1
|    |--DataBindingRenderer item with content2
|    |--...
|
|--DataBindingRenderer footer
```

由于构建出来的Adapter的泛型中包含有整个列表树的结构信息, 因此可以通过反推的方式提取其中的结构信息, 目前以这种手法实现了两个工具:

1. `DSL更新器`: 可以进行部分更新 (需要`dsladapter-updater`模块)

```kotlin
adapter.updateNow {
    getLast2().up(::updatable) {
        update(provideData(index))
    } + getLast3().up(::updatable) {
        move(2, 4) +
        subs(3)(::updatable) {
            update(ItemModel(189, "Subs Title $index", "subs Content"))
        }
    } + getLast4().up(::updatable) {
        getLast1().up(::updatable) {
            update(provideData(index))
        }
    }
}
```

也支持`DiffUtil`工具:

```kotlin
adapter.updateNow(::updatable) { // ListUpdater
    updateAuto(newData, diffUtilCheck { it.id })
}
```

2. `DSL position获取器`: 可以获取结构中某个位置的Renderer数据的在全局坐标中的位置 (需要`dsladapter-position`模块)

```kotlin
val pos = adapter.renderer.pos {
            subsPos(10) {
                sealedItem({ this.head })
            }
        }(adapter.getViewData())

// pos is RendererPos(start, count)
```

---

### Ver.2.0 新增

为类型很复杂的`ComposeRenderer`和`SealedItemRenderer`两种Renderer新增了两种简化的替代品。
如果你不需要`ComposeRenderer`和`SealedItemRenderer`带来的强类型安全的话可以尝试使用它们。

#### SplitRenderer

去除强静态类型的 ComposeRenderer，构造上在于把一个数据分发给多个子Renderer，多个子Renderer按顺序进行渲染。

```
Data --|--> Renderer1<Data>
       |--> Renderer2<Data>
       ...
```

Sample:

```kotlin
SplitRenderer.build<String> {
    +LayoutRenderer<String>(layout = R.layout.sample_layout)

    !LayoutRenderer<Unit>(layout = R.layout.sample_layout)
}
```

#### CaseRenderer

去除了强静态类型的SealedItemRenderer，构造上为根据判定式选择第一个符合的Case进行渲染。

Sample:

```kotlin
CaseRenderer.build<String> {
    caseItem(CaseItem(
            checker = { true },
            mapper = { it },
            demapper = awaysReturnNewData(),
            renderer = LayoutRenderer(layout = 1)
    ))

    caseItemSame(CaseItem(
            checker = { true },
            mapper = { it },
            demapper = awaysReturnNewData(),
            renderer = LayoutRenderer(layout = 1)
    ))

    case(checker = { true },
            mapper = { Unit },
            renderer = LayoutRenderer<Unit>(layout = 1))

    case(checker = { true },
            demapper = doNotAffectOriData(),
            renderer = LayoutRenderer(layout = 1))

    // 所有条件均不满足的情况下执行elseCase
    elseCase(mapper = { Unit },
            renderer = LayoutRenderer<Unit>(layout = 1))

    // elseCase只能设置一个，如果设置多次、以最后的设定为准
    elseCase(renderer = LayoutRenderer(layout = 1))
}
```

---

## 详细使用说明

### 基础Renderer

DslAdapter这个库的基本组合子是`BaseRenderer`, 复杂的结构都是通过对基本Renderer进行组合而创造的

以下是一些默认实现的基本Renderer, 基本可以满足大部分需求, 另外也可以通过实现`BaseRenderer`类的方式扩展自己的Renderer

1. **EmptyRenderer**: 空Renderer, count为0
2. **LayoutRenderer**: 与View绑定的末端Renderer, 可自定义数量
3. **ConstantItemRenderer**: 将常量绑定到View的末端Renderer, 可适配任意数据源, 可自定义数量
4. **MapperRenderer**: 转换目标Renderer的数据源类型, 一般通过`mapT()`来使用它
5. **ListRenderer**: 将目标Renderer转换为适配列表数据源
6. **SealedItemRenderer**: 根据数据源具体数据选择不同的Renderer渲染, 比如对于`Int?`类型，可以在为`null`的时候用EmptyRenderer渲染; 不为`null`的时候使用LayoutRenderer渲染
7. **ComposeRenderer**: 组合多个不同Renderer
8. **DataBindingRenderer** : Android Databinding支持的Renderer
9. **TitleItemRenderer** : 一个Title Renderer加列表的Renderer. `现已废弃`, 请使用ComposeRenderer代替.
10. **SplitRenderer**: 去除了强静态类型的ComposeRenderer
11. **CaseRenderer**: 去除了强静态类型的SealedItemRenderer


### Part1 基本Renderer的使用示例

#### 1.1 LayoutRenderer
```kotlin
val stringRenderer = LayoutRenderer<String>(layout = R.layout.simple_item,
        count = 3,
        binder = { view, title, index -> view.findViewById<TextView>(R.id.simple_text_view).text = title + index },
        recycleFun = { view -> view.findViewById<TextView>(R.id.simple_text_view).text = "" })
```

#### 1.2 DataBindingRenderer
```kotlin
val itemRenderer = databindingOf<ItemModel>(R.layout.item_layout)
        .onRecycle(CLEAR_ALL)
        .itemId(BR.model)
        .itemId(BR.content, { m -> m.content + "xxxx" })
        .stableIdForItem { it.id }
        .forItem()
```

#### 1.3 TitleItemRenderer (`现已废弃`, 请使用ComposeRenderer代替.)
一个Title Renderer加列表的Renderer:

```
|-- Title  (eg: stringRenderer)
|-- Sub1   (eg: itemRenderer)
|-- Sub2
|-- ...
```

```kotlin
val renderer = TitleItemRenderer(
        itemType = type<List<ItemModel>>(),
        titleGetter = { "title" },
        subsGetter = { it },
        title = stringRenderer,
        subs = itemRenderer)
```

#### 1.4 SealedItemRenderer
根据数据源具体数据选择不同的Renderer渲染, 通过由上到下调用`check`函数选择使用的item

比如要构建一个类似这样选择条件的Renderer:
```
when {
  list.isEmpty -> stringRenderer
  list.isNotEmpty -> list of itemRenderer
}
```

可以这样构建:
```kotlin
val rendererSealed = SealedItemRenderer(hlistKOf(
        item(type = type<List<ItemModel>>(),
                checker = { it.isEmpty() },
                mapper = { "empty" },
                demapper = doNotAffectOriData(),
                renderer = stringRenderer
        ),
        item(type = type<List<ItemModel>>(),
                checker = { it.isNotEmpty() },
                mapper = { it },
                demapper = { oldData, newSubData -> newSubData },
                renderer = itemRenderer.forList()
        )
))
```

##### SplitRenderer

去除强静态类型的 ComposeRenderer，构造上在于把一个数据分发给多个子Renderer，多个子Renderer按顺序进行渲染。

```
Data --|--> Renderer1<Data>
       |--> Renderer2<Data>
       ...
```

Sample:

```kotlin
SplitRenderer.build<String> {
    +LayoutRenderer<String>(layout = R.layout.sample_layout)

    !LayoutRenderer<Unit>(layout = R.layout.sample_layout)
}
```

#### 1.5 ComposeRenderer

组合多个不同Renderer, eg:

```
|-- item1 (eg: itemRenderer)
|-- item2 (eg: stringRenderer)
```

```kotlin
val composeRenderer = ComposeRenderer.startBuild
        .add(itemRenderer)
        .add(stringRenderer)
        .build()
```

##### CaseRenderer

去除了强静态类型的SealedItemRenderer，构造上为根据判定式选择第一个符合的Case进行渲染。

Sample:

```kotlin
CaseRenderer.build<String> {
    caseItem(CaseItem(
            checker = { true },
            mapper = { it },
            demapper = awaysReturnNewData(),
            renderer = LayoutRenderer(layout = 1)
    ))

    caseItemSame(CaseItem(
            checker = { true },
            mapper = { it },
            demapper = awaysReturnNewData(),
            renderer = LayoutRenderer(layout = 1)
    ))

    case(checker = { true },
            mapper = { Unit },
            renderer = LayoutRenderer<Unit>(layout = 1))

    case(checker = { true },
            demapper = doNotAffectOriData(),
            renderer = LayoutRenderer(layout = 1))

    // 所有条件均不满足的情况下执行elseCase
    elseCase(mapper = { Unit },
            renderer = LayoutRenderer<Unit>(layout = 1))

    // elseCase只能设置一个，如果设置多次、以最后的设定为准
    elseCase(renderer = LayoutRenderer(layout = 1))
}
```

---

### Part2 Adapter的构建

DslAdapter的核心Adapter是`RendererAdapter`, 虽然Renderer本身可以单独构建, 但Renderer构建时不会包括初始数据, 为了方便, 有以下的Builder构造器可以直接用:

> Tip1: RendererAdapter中只有一个renderer

> Tip2: 构建时需要与Renderer类型相同的初始数据, 如果想要初始数据为`null`, 则请构建能够支持`null`数据类型的Renderer

1. constructor
2. singleRenderer
3. multiple
4. multipleBuild
5. splitBuild
6. singleSupplier
7. supplierBuilder
8. singleRxAutoUpdate
9. rxBuild

#### 2.1 使用constructor构造

```kotlin
val adapterDemo1 = RendererAdapter(
        initData = HListK.singleId(emptyList<ItemModel>()).putF("ss"),
        renderer = ComposeRenderer.startBuild
                .add(renderer)
                .add(stringRenderer)
                .build()
)
```

#### 2.2 使用`singleRenderer()`方法构建
```kotlin
val adapterDemo2 = RendererAdapter.singleRenderer(
        initData = HListK.singleId(emptyList<ItemModel>()).putF("ss"),
        renderer = ComposeRenderer.startBuild
                .add(renderer)
                .add(stringRenderer)
                .build()
)
```

#### 2.3 使用`multiple()`方法构建

```kotlin
val adapterDemo3 = RendererAdapter.multiple(HListK.singleId(emptyList<ItemModel>()).putF("ss"))
{
    start
            .add(rendererSealed)
            .add(stringRenderer)
}
```

#### 2.4 使用`multipleBuilder()`方法构建

```kotlin
val adapterDemo4 = RendererAdapter.multipleBuild()
        .add(emptyList<ItemModel>(), rendererSealed)
        .add("ss", stringRenderer)
        .build()
```

#### 2.5 使用`splitBuild`方法构建

```kotlin
val adapterDemo42 = RendererAdapter.splitBuild(emptyList<ItemModel>()) {
    +rendererSealed

    !unitRenderer
}
```

#### 2.6 使用`singleSupplier()`方法构建

```kotlin
val (adapter3, controller1) = RendererAdapter
        .singleSupplier({ provideData(index) }, renderer)
```

#### 2.7 使用`supplierBuilder()`方法构建

```kotlin
val (adapter2, controller) = RendererAdapter.supplierBuilder()
        .addStatic(layout<Unit>(R.layout.list_header))
        .add({ provideData(index) }, renderer)
        .build()
```

#### 2.8 使用`singleRxAutoUpdate()`方法构建 `Beta`

```kotlin
RendererAdapter.singleRxAutoUpdate(dataProvider, renderer)
{ adapter ->
    recyclerView.adapter = adapter
}.subscribe()
```

#### 2.9 By 使用`rxBuild()`Builder构建 `Beta`

```kotlin
RendererAdapter.rxBuild()
        .addStatic(layout<Unit>(R.layout.list_header))
        .add(dataProvider, renderer)
        .buildAutoUpdate { adapter ->
            recyclerView.adapter = adapter
        }
        .subscribe()
```

---

## Part3 数据更新

#### 3.1 使用`setData()`更新

最简单的是使用`setData()`进行更新

> 注意这种方式会使用`notifyDataSetChanged()`方法通知Adapter数据改变

```kotlin
adapterDemo1.setData(HListK.singleId(listOf(ItemModel())).putF("ss2"))
```

#### 3.2 或者使用`reduce()`方法更新

> 注意这种方式会使用`notifyDataSetChanged()`方法通知Adapter数据改变

```kotlin
adapterDemo1.reduceData { oldData -> oldData.map1 { "ss3".toIdT() } }
```

#### 3.3 或者使用`setDataAuto()`方法更新

这个方法会自动根据数据的变化自动计算部分更新进行更新，不是使用`notifyDataSetChanged()`

```kotlin
adapterDemo1.setDataAuto(listOf(ItemModel()))
```

> 除了以上两种强制更新的方法, 还可以进行部分更新(通过`notifyItemRangeInserted`等方法进行更新):

#### 3.3 使用`update()`方法

方法中通过DSL方式构建自定义的更新计算方式

This function will return a UpdateResult, this time not really update data for adapter.
Please use [dispatchUpdatesTo()] to apply update action to adapter

```kotlin
adapterDemo1.update {
    getLast2().up(::updatable) {
        title(::updatable) {
            update("new Title-${random.nextInt()}")
        }
    }
}.dispatchUpdatesTo(adapterDemo1)
```

其中`update`方法会返回一个`UpdateResult`, 这时只是计算了新旧数据之间的差异, 并没有实际更新视图(因此这一步可以放在计算线程计算);
而通过`dispatchUpdatesTo`方法可以将计算结果实际应用到Adapter上, **这一步一定要在主线程执行**

#### 3.4 通过`updateNow()`方法更新

此方法不像上面的`update`方法, 它不会返回计算结果`UpdateResult`而是直接将计算结果应用到Adapter, 适用于计算量不大的更新场合.

**此方法请一定要在主线程执行**

```kotlin
adapterDemo1.updateNow {
    getLast2().up(::updatable) {
        title(::updatable) {
            update("new Title-${random.nextInt()}")
        }
    }
}
```

### 自动更新

> 注意!: `3.5`和`3.6`更新方法目前都是使用`update`进行更新, 即通过`notifyDataSetChanged()`方法通知Adapter数据改变

#### 3.5 使用 `Supplier` 数据源

使用`supplierBuilder()`或者`singleSupplier()`来构建Adapter, 然后使用controller来控制强制数据刷新

构建 Supplier Adapter:
```kotlin
// 1
val (adapter3, controller1) = RendererAdapter
        .singleSupplier({ provideData(index) }, renderer)

// 2
val (adapter2, controller) = RendererAdapter.supplierBuilder()
        .addStatic(layout<Unit>(R.layout.list_header))
        .add({ provideData(index) }, renderer)
        .build()
```

使用 controller 来更新数据(调用此方法的时候会触发获取数据的方法来获取新的数据)
```kotlin
controller.updateAll()
```

#### 3.6 使用 RxJava(Observable) 数据源 `Beta`

可以使用`singleRxAutoUpdate()`或者`rxBuild()`来构建以RxJava作为数据源的Adapter:

```kotlin
val dataProvider = PublishSubject.create<List<ItemModel>>()

// 1
RendererAdapter.rxBuild()
        .addStatic(layout<Unit>(R.layout.list_header))
        .add(dataProvider, renderer)
        .buildAutoUpdate { adapter ->
            recyclerView.adapter = adapter
        }
        .subscribe()

// 2
RendererAdapter.singleRxAutoUpdate(dataProvider, renderer)
{ adapter ->
    recyclerView.adapter = adapter
}.subscribe()
```

Adapter将会在Observable有新数据的时候自动更新数据

```kotlin
dataProvider.onNext(newData)
```

### 对RxJava数据源进行部分更新的实现示例:

```kotlin
// Util func
fun <T, P, VD : ViewData<P>> Observable<T>.updateTo(
        adapter: RendererAdapter<P, VD>,
        updater: BaseRenderer<P, VD>.(newData: T) -> ActionU<VD>): Completable =
        distinctUntilChanged().concatMap {
            observeOn(Schedulers.computation())
                    .map { newData -> adapter.update { updater(newData) } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        it.dispatchUpdatesTo(adapter)
                    }
        }.ignoreElements()
```

使用方法

```kotlin
dataProvider3
        .updateTo(adapter)
        {
            updater.getLast4().up(::updatable) {
                sealedItem({ get0().fix() }, ::updatable) {
                    update(listOf(ItemModel().some(), none<ItemModel>()))
                }
            }
        }.subscribe()
```




### License
<pre>
Copyright 2018 Yumenokanata

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
