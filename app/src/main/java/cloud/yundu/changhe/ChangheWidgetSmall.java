package cloud.yundu.changhe;

/** 小尺寸 2 × 2，独立 provider，让选择器里多出一种尺寸。 */
public final class ChangheWidgetSmall extends ChangheWidget {
    @Override protected int layoutId() { return R.layout.widget_small; }
    @Override protected Class<?> providerClass() { return ChangheWidgetSmall.class; }
}
