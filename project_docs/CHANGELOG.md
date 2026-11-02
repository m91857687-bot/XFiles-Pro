# XFiles Pro — CHANGELOG (سجل التعديلات والإصدارات)

جميع التعديلات والتحسينات البرمجية التي تطرأ على مشروع **XFiles Pro** يتم توثيقها زمنياً في هذا الملف وفق معايير [Keep a Changelog](https://keepachangelog.com/).

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
