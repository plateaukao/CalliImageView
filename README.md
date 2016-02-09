## CalliImageView

A custom view for android platform to display Chinese calligraphy
characters with extra features.

## Features
1. fill type vs. contour type
characters with contour lines are more suitable for practicing
hand writings directly on screen.

2. grip type: 9 grids vs diagonal grid
Different grid styles are provided.

3. show/hide character
without character, you can practice writing directly on grid

## Demo
<img
src="https://lh3.googleusercontent.com/QPT4p17z-azO9z8qYDyYu9QZhmh68uj0naGP4jaGtBT30nC4xJuN9ihgEyEZVLXLLoly-EkOMcki3g=w390-h692-no" width="360">

## How to use the library

in build.gradle, include jcenter()
```gradle
buildscript {
    repositories {
        jcenter()
    }
}
```

add following dependency
```gradle
dependencies {
    ...
    compile 'info.plateaukao.android:customviews:1.0.0'
    ...
}
```
## Add CalliImageView in layout file

```xml
<info.plateaukao.android.customviews.CalliImageView
    android:id="@+id/char_imageview"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_alignParentTop="true"
    android:layout_gravity="center"
    custom:borderSize="@dimen/char_border_size"
    custom:crossSize="@dimen/char_cross_size"
    android:background="@color/char_bg"
    android:gravity="center"
    android:scaleType="fitCenter" />
```
