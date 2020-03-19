# DslAdapter
[![](https://jitpack.io/v/Yumenokanata/DslAdapter.svg)](https://jitpack.io/#Yumenokanata/DslAdapter)

[中文文档](https://github.com/Yumenokanata/DslAdapter/blob/master/README_CN.md)

A RecyclerView Adapter builder by DSL. Easy to use, **type safety**, and all code written by kotlin.

* Type safety by HList (Heterogeneous lists)
* Build DSL
* Update DSL

---

<img src="https://raw.githubusercontent.com/Yumenokanata/DslAdapter/master/screen/tree.gif" width="260" height="480">    <img src="https://raw.githubusercontent.com/Yumenokanata/DslAdapter/master/screen/compose.gif" width="260" height="480">    <img src="https://raw.githubusercontent.com/Yumenokanata/DslAdapter/master/screen/drag.gif" width="260" height="480">

---

Add this in your root build.gradle at the end of repositories:
```groovy
allprojects {
	repositories {
        jcenter()
		maven { url "https://jitpack.io" }
	}
}
```

Step 2. Add the dependency in your module's gradle file

Since Ver2.0:

```groovy
dependencies {
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter:x.y.z'
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-rx2:x.y.z' // Beta, optional
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-position:x.y.z' // optional
}
```

Before Ver2.0:

```groovy
dependencies {
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter:x.y.z'
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-updater:x.y.z'
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-rx2:x.y.z' // Beta, optional
    implementation 'com.github.Yumenokanata.DslAdapter:dsladapter-position:x.y.z' // optional
}
```

[Sample](https://github.com/Yumenokanata/DslAdapter/blob/master/sample/src/main/java/indi/yume/tools/sample/MainActivity.kt)


---

Now you can simply build a complex RecyclerView structure, just by:

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

Part Update by DSL: (`dsladapter-updater` module)

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

`DiffUtil` support:

```kotlin
adapter.updateNow(::updatable) { // ListUpdater
    updateAuto(newData, diffUtilCheck { it.id })
}
```

Position get by DSL: (`dsladapter-position` module)

```kotlin
val pos = adapter.renderer.pos {
            subsPos(10) {
                sealedItem({ this.head })
            }
        }(adapter.getViewData())

// pos is RendererPos(start, count)
```

---

### Ver.2.1 New

#### TreeExtension

Make tree adapter by DSL:

```kotlin
val sampleFiles = TreeNodeBuilder.buildTree<FolderNode.Folder, FolderNode.File> {
    addLeaf("file1", FolderNode.File)

    addNode("program", FolderNode.Folder) {
        addLeaf("clear.sh", FolderNode.File)
        addLeaf("thumbnail.db", FolderNode.File)
        addNode("AndroidStudio", FolderNode.Folder) {
            addLeaf("studio32.sh", FolderNode.File)
            addLeaf("studio64.sh", FolderNode.File)
        }
        addLeaf("temp.db", FolderNode.File)
    }

    // Infinitely recursive lazy folder
    // 无限递归的惰性文件夹
    lateinit var subCreator: TreeBuilderFun<FolderNode.Folder, FolderNode.File>
    var times = 0
    subCreator = {
        addNodeLazy("Infinitely Folder${times++}", FolderNode.Folder, isOpen = false) {
            addLeaf("img1.jpg", FolderNode.File)
            addLeaf("img2.jpg", FolderNode.File)
            subCreator()
        }
    }
    subCreator()

    addLeaf("file2", FolderNode.File)
    addLeaf("file3", FolderNode.File)

    addNode("projects", FolderNode.Folder) {
        addNode("DslAdapter", FolderNode.Folder) {
            addLeaf("Readme.md", FolderNode.File)
        }
    }

    addNode("picture", FolderNode.Folder) {
        addLeaf("img1.jpg", FolderNode.File)
        addLeaf("img2.jpg", FolderNode.File)
        addLeaf("img3.jpg", FolderNode.File)
    }
}
```

Make Adapter

```koltin
RendererAdapter.singleRenderer(sampleFiles,
                treeRenderer(folderItemRenderer, fileRenderer))
```

Or use `treeRenderer()` to compose with other Renderer

### Ver.2.0 New

Make new simplify Renderer for `ComposeRenderer` and `SealedItemRenderer`, If you don't need to use strong static typing, use them first

#### SplitRenderer

No high type ComposeRenderer.

Sample:

```kotlin
SplitRenderer.build<String> {
    +LayoutRenderer<String>(layout = R.layout.sample_layout)

    !LayoutRenderer<Unit>(layout = R.layout.sample_layout)
}
```

#### CaseRenderer

No strong static typing type SealedItemRenderer

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

    elseCase(mapper = { Unit },
            renderer = LayoutRenderer<Unit>(layout = 1))

    elseCase(renderer = LayoutRenderer(layout = 1))
}
```

---

### Base Renderer

1. **EmptyRenderer**
2. **LayoutRenderer**
3. **ConstantItemRenderer**
4. **MapperRenderer**
5. **ListRenderer**: Make list for other renderer
6. **SealedItemRenderer**: Choice different Renderer for different data.
7. **ComposeRenderer**
8. **DataBindingRenderer** : Bind with Android Databinding
9. **TitleItemRenderer** : Deprecated, please instead by ComposeRenderer.
10. **SplitRenderer**: No strong static typing type ComposeRenderer
11. **CaseRenderer**: No strong static typing type SealedItemRenderer

Use this Base Renderers, you can Make a complex RecyclerView structure:

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

### Part1 Make Renderer

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

#### 1.3 TitleItemRenderer

`Deprecated， Please instead by ComposeRenderer.`

This Renderer will build a title with subs, like:

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
This Renderer will build a sealed Renderer, if checker is 'right' will show this renderer, eg:

```
when {
  list.isEmpty -> stringRenderer
  list.isNotEmpty -> list of itemRenderer
}
```

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

##### CaseRenderer

No strong static typing type SealedItemRenderer

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

    elseCase(mapper = { Unit },
            renderer = LayoutRenderer<Unit>(layout = 1))

    elseCase(renderer = LayoutRenderer(layout = 1))
}
```

#### 1.5 ComposeRenderer

This Renderer will compose all item renderer, eg:

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

##### SplitRenderer

No high type ComposeRenderer.

Sample:

```kotlin
SplitRenderer.build<String> {
    +LayoutRenderer<String>(layout = R.layout.sample_layout)

    !LayoutRenderer<Unit>(layout = R.layout.sample_layout)
}
```

---

### Part2 BuildAdapter

> Tips: RendererAdapter only have one renderer

1. constructor
2. singleRenderer
3. multiple
4. multipleBuild
5. splitBuild
6. singleSupplier
7. supplierBuilder
8. singleRxAutoUpdate
9. rxBuild

#### 2.1 By constructor

```kotlin
val adapterDemo1 = RendererAdapter(
        initData = HListK.singleId(emptyList<ItemModel>()).putF("ss"),
        renderer = ComposeRenderer.startBuild
                .add(renderer)
                .add(stringRenderer)
                .build()
)
```

#### 2.2 By singleRenderer func
```kotlin
val adapterDemo2 = RendererAdapter.singleRenderer(
        initData = HListK.singleId(emptyList<ItemModel>()).putF("ss"),
        renderer = ComposeRenderer.startBuild
                .add(renderer)
                .add(stringRenderer)
                .build()
)
```

#### 2.3 By multiple func

```kotlin
val adapterDemo3 = RendererAdapter.multiple(HListK.singleId(emptyList<ItemModel>()).putF("ss"))
{
    start
            .add(rendererSealed)
            .add(stringRenderer)
}
```

#### 2.4 By multiple Builder

```kotlin
val adapterDemo4 = RendererAdapter.multipleBuild()
        .add(emptyList<ItemModel>(), rendererSealed)
        .add("ss", stringRenderer)
        .build()
```

#### 2.5 By split Builder

```kotlin
val adapterDemo42 = RendererAdapter.splitBuild(emptyList<ItemModel>()) {
    +rendererSealed

    !unitRenderer
}
```

#### 2.6 By singleSupplier Builder

```kotlin
val (adapter3, controller1) = RendererAdapter
        .singleSupplier({ provideData(index) }, renderer)
```

#### 2.7 By supplierBuilder Builder

```kotlin
val (adapter2, controller) = RendererAdapter.supplierBuilder()
        .addStatic(layout<Unit>(R.layout.list_header))
        .add({ provideData(index) }, renderer)
        .build()
```

#### 2.8 By singleRxAutoUpdate Builder `Beta`

```kotlin
RendererAdapter.singleRxAutoUpdate(dataProvider, renderer)
{ adapter ->
    recyclerView.adapter = adapter
}.subscribe()
```

#### 2.9 By rxBuild Builder `Beta`

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

## Part3 Update Data

#### 3.1 Simple method is setData()

> Use notifyDataSetChanged

```kotlin
adapterDemo1.setData(HListK.singleId(listOf(ItemModel())).putF("ss2"))
```

#### 3.2 Or use reduce method

> Use notifyDataSetChanged

```kotlin
adapterDemo1.reduceData { oldData -> oldData.map1 { "ss3".toIdT() } }
```

#### 3.3 Or auto part update data

```kotlin
adapterDemo1.setDataAuto(listOf(ItemModel()))
```

### Two way to part update data:

#### 3.3 By update() func

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

#### 3.4 By updateNow() func

Unlike the update method, this method will apply the update directly to the Adapter.

```kotlin
adapterDemo1.updateNow {
    getLast2().up(::updatable) {
        title(::updatable) {
            update("new Title-${random.nextInt()}")
        }
    }
}
```

### Auto Update

#### 3.5 Bind Supplier

Use supplierBuilder() or singleSupplier() to build adapter, and use controller to force update.

Build Supplier Adapter:
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

Use controller to force update.
```kotlin
controller.updateAll()
```

#### 3.6 Bind RxJava(Observable) `Beta`

Use singleRxAutoUpdate() or rxBuild() to build adapter:

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

Adapter updates automatically when Observable's data is updated.

```kotlin
dataProvider.onNext(newData)
```

#### 3.7 Bind RxJava(Observable) by part update

eg:

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
