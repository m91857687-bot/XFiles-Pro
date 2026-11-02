# XFiles Pro — CHANGELOG (سجل التعديلات والإصدارات)

جميع التعديلات والتحسينات البرمجية التي تطرأ على مشروع **XFiles Pro** يتم توثيقها زمنياً في هذا الملف وفق معايير [Keep a Changelog](https://keepachangelog.com/).

## [0.1.0-alpha.4] - 2026-11-02 (المجموعة الأولى - المرحلة 4: الواجهة الرئيسية وهيكل التنقل)

### تمت إضافته (Added)
- **هيكل التنقل التكيفي والوجهات الخمس (`ui/navigation/`):**
  - `NavDestination.kt`: تعداد نوعي آمن للوجهات الخمس (`HOME`، `FILES`، `MEDIA`، `TOOLS`، `SETTINGS`) مع أيقونات الحالة العادية والمحددة والمسميات المترجمة.
  - `XFilesAppShell.kt`: الحاوية الرئيسية للتطبيق مع دعم التكيف بين `NavigationBar` للهواتف و `NavigationRail` للشاشات العريضة والأجهزة اللوحية (`>= 600dp`).
  - دعم زر الرجوع الذكي `BackHandler` للعودة التلقائية إلى الواجهة الرئيسية قبل الخروج.
- **الشريط العلوي الموحد (`ui/components/XFilesTopAppBar.kt`):**
  - عرض هوية XFiles Pro، عنوان القسم الحالي، زر ربط البحث الفوري، زر تبديل وضع العرض (List/Grid) بأيقونات AutoMirrored، وقائمة الخيارات الإضافية.
- **الشاشات الرئيسية الخمس:**
  - `HomeScreen.kt`: بطاقة التخزين المتقدمة (المساحة الإجمالية، المستخدمة، المتبقية، ونسبة الاستهلاك)، شبكة الوصول السريع لـ 8 أقسام (الذاكرة الداخلية، بطاقة SD، الصور، الفيديوهات، الصوتيات، المستندات، التنزيلات، والتطبيقات)، ومعاينة الملفات الأخيرة.
  - `FilesScreen.kt`: شريط مسار المجلدات التفاعلي (Breadcrumbs) مع التمرير الأفقي والقفز للمجلدات، ودعم التبديل السلس بين عرض القائمة وعرض الشبكة.
  - `MediaScreen.kt`: شاشة الوسائط المنظمة بـ 3 تبويبات (الصور، الفيديوهات، الصوتيات) مع شبكة وسائط واستعراض البيانات ومدد المقاطع.
  - `ToolsScreen.kt`: بطاقات الأدوات المتقدمة المجدولة للمراحل القادمة (محلل التخزين، منظف التكرار، سلة المحذوفات، المشاركة عبر Wi-Fi، والخزنة المشفرة).
  - `SettingsScreen.kt`: تكامل مباشر مع `LanguageViewModel` و `ThemeViewModel`، وفتح `LanguagePickerDialog` و `ThemePickerDialog` بسلاسة، مع عرض إصدار التطبيق وبنية المعمارية.
- **تحديث الموارد وسلاسل الترجمة (Localization Strings):**
  - إضافة مفاتيح النصوص الكاملة للواجهة الرئيسية عبر اللغات الأربع (ar, en, es, fa) في `values/`, `values-ar/`, `values-es/`, `values-fa/` مع تطابق 100%.
- **حزم الاختبارات (17 اختباراً جديداً — الإجمالي 75 اختباراً بنسبة نجاح 100%):**
  - `NavDestinationTest.kt`: 5 اختبارات للتحقق من سلامة تعداد الوجهات واستخراجها والتفرد.
  - `NavigationAndShellRobolectricTest.kt`: 7 اختبارات تفحص تنقل التبويبات، شبكة الوصول السريع، تكامل اللغات الأربع، اتجاهات RTL/LTR، وتركيبات السمات.
  - `NavigationStateAndBackHandlerTest.kt`: 5 اختبارات تفحص إدارة حالة التنقل، عمل زر الرجوع والعودة للرئيسية، وتبديل أوضاع العرض (List/Grid).

---

## [0.1.0-alpha.3] - 2026-11-02 (المجموعة الأولى - المرحلة 3: نظام السمات البصرية)

### تمت إضافته (Added)
- **السمات البصرية الأربع:**
  - Classic: واجهة أبيض وأسود عالية التباين (الافتراضية).
  - Midnight: واجهة داكنة مريحة لشاشات AMOLED مع لمسات جليدية (#60A5FA).
  - Royal: واجهة أوبسيديان مخملية فخمة مع لمسات ذهبية ملكية (#D4AF37 / #FFD700).
  - System: تتبع تلقائي فوري لمظهر نظام أندرويد مع Dynamic Colors.
- **نظام Color Tokens المركزي (`ColorTokens.kt`):**
  - توحيد 13 رمزاً لونياً وتوفيرها عبر `LocalXFilesTokens` لضمان عدم وجود ألوان متناثرة.
- **طبقة النماذج والبيانات (Domain & Data Layer):**
  - `ThemeMode.kt`: نموذج يمثل السمات الأربع ومواردها النصية.
  - `ThemeRepository.kt` & `ThemeRepositoryImpl.kt`: عقد وتنفيذ إدارة تدفق السمة عبر DataStore.
  - `ThemePreferences.kt`: تخزين تفضيلات السمة مع إعادة استخدام كائن DataStore الموحد دون ازدواجية.
  - تحديث `AppContainer.kt` لتوفير `themePreferences` و `themeRepository`.
- **طبقة واجهة المستخدم (UI Layer):**
  - `ThemeUiState.kt` & `ThemeViewModel.kt`: إدارة تفضيل السمة والنافذة الحوارية.
  - `ThemePickerDialog.kt`: حوار اختيار السمة مع معاينة حية للألوان (Mini Palette Swatches).
  - تحديث `XFilesTheme`: استهلاك ديناميكي لـ `ThemeMode` بدون وميض وبدون إعادة تشغيل النشاط.
- **حزم الاختبارات (29 اختباراً جديداً — الإجمالي 58 اختباراً بنسبة نجاح 100%):**
  - `ThemeModeTest.kt` (5 اختبارات).
  - `ColorTokensTest.kt` (5 اختبارات).
  - `ThemeRepositoryTest.kt` (3 اختبارات).
  - `RealDataStoreThemeTest.kt` (4 اختبارات).
  - `ThemeViewModelTest.kt` (6 اختبارات).
  - `SystemThemeDynamicResponseTest.kt` (2 اختبار).
  - `ThemeAndLocalizationCombinationsTest.kt` (4 اختبارات تفحص كافة التركيبات الـ 16 بين اللغات والسمات).

---

## [0.1.0-alpha.2] - 2026-11-02 (المجموعة الأولى - المرحلة 2: نظام اللغات الشامل)

### تمت إضافته (Added)
- **موارد الترجمة المركزية (Localization Resources):**
  - إنشاء `res/values/strings.xml` (المرجع الإنجليزي).
  - إنشاء `res/values-ar/strings.xml` (الترجمة العربية الاحترافية الكاملة).
  - إنشاء `res/values-es/strings.xml` (الترجمة الإسبانية الكاملة).
  - إنشاء `res/values-fa/strings.xml` (الترجمة الفارسية الكاملة).
  - تغطية 100% لجميع المفاتيح عبر اللغات الأربع دون أي نصوص صريحة (Zero Hardcoded Strings).
- **طبقة النماذج ومنطق الأعمال (Domain Layer):**
  - `AppLanguage.kt`: Enum يمثل اللغات الأربع (ar, en, es, fa) مع أسمائها الأصلية وخصائص اتجاه الكتابة (isRtl).
  - `LanguageRepository.kt`: واجهة إدارة تدفق اللغة المختارة وتتبع حالة أول تشغيل.
- **طبقة البيانات والتخزين الدائم (Data Layer):**
  - `LanguagePreferences.kt`: إدارة التخزين المستمر باستخدام Jetpack DataStore (Preferences).
  - `LanguageRepositoryImpl.kt`: تطبيق المستودع مع تشغيل عمليات القراءة والكتابة على `Dispatchers.IO`.
  - تحديث `AppContainer.kt` لتوفير `languagePreferences` و `languageRepository`.
- **طبقة واجهة المستخدم (UI Layer):**
  - `LanguageUiState.kt`: فئة حالة واجهة اللغة وإدارة حالة النوافذ الحوارية.
  - `LanguageViewModel.kt`: إدارة التفاعلات وحفظ التفضيلات فورياً.
  - `LanguageSelectionScreen.kt`: شاشة مستقلة مخصصة لاختيار اللغة عند أول تشغيل فقط، تعرض الأسماء الأصلية مع بطاقات تفاعلية وتمنع التجاوز دون اختيار.
  - `LanguagePickerDialog.kt`: نافذة حوارية سريعة تتيح تغيير لغة التطبيق أثناء التشغيل دون الحاجة لشاشة إعدادات كاملة.
  - `LocalizedApp.kt`: مغلف Compose يحقن السياق المترجم و `Configuration` و `LocalLayoutDirection` ديناميكياً لتحديث الواجهة فورياً دون إعادة تشغيل الـ Activity.
- **حزم الاختبارات (17 اختباراً جديداً — الإجمالي 27 اختباراً):**
  - `AppLanguageTest.kt`: اختبارات اتجاهات RTL/LTR و fromCode وأسماء اللغات (5 اختبارات).
  - `LanguageRepositoryTest.kt`: اختبارات تدفق DataStore وحفظ التفضيلات وحالة أول تشغيل (4 اختبارات).
  - `LanguageViewModelTest.kt`: اختبارات تفاعل وحالات ViewModel والنافذة الحوارية (4 اختبارات).
  - `RobolectricLocalizationTest.kt`: اختبارات Robolectric للتحقق من حل النصوص باللغات الأربع بدقة (4 اختبارات).

### تم تعديله (Changed)
- **`MainActivity.kt`:**
  - دمج `LanguageViewModel` و `LocalizedApp`.
  - توجيه المستخدم لشاشة اختيار اللغة عند أول تشغيل، ثم للشاشة الرئيسية مباشرة بعد الحفظ الدائم.
  - إضافة بطاقة استعراض حالة اللغة والاتجاه وزر التبديل السريع.

---

## [0.1.0-alpha.1] - 2026-11-02 (المجموعة الأولى - المرحلة 1: تأسيس المشروع والمعمارية)
- توحيد namespace و applicationId على `com.xfiles.pro`.
- إنشاء طبقات core و domain و data ونماذج الملفات الأساسية و Room Database التأسيسية.
- إعداد بنية الاختبارات الأولية.

---

## [0.1.0-alpha.0] - 2026-11-02
- الإعداد التوثيقي المبدئي وملفات هوية التطبيق.
