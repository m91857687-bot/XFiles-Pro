# XFiles Pro — PROJECT STRUCTURE (بنية وهيكل مجلدات المشروع)

يشرح هذا المستند التقسيم الهيكلي لحزم المشروع وملفات المصدر والموارد بعد اكتمال المرحلتين الأولى والثانية.

---

## 1. الهيكل العام لجذور المشروع (Root Directory Layout)
```text
/
├── project_docs/              # ملفات التوثيق الهندسية العشرة الدائمة للمشروع
│   ├── MASTER_CONTEXT.md      # السياق العام وقواعد المشروع
│   ├── FEATURES_SPEC.md       # مواصفات جميع الميزات الـ 23
│   ├── ARCHITECTURE.md        # المعمارية والتقنيات وتصميم الأنظمة
│   ├── PROJECT_STRUCTURE.md   # شرح هيكل المجلدات والملفات (هذا الملف)
│   ├── PHASE_PLAN.md          # خطة المراحل الـ 15 وترتيبها
│   ├── PROJECT_STATUS.md      # الحالة التنفيذية الدقيقة للمشروع
│   ├── DECISIONS.md           # سجل القرارات الهندسية (ADRs)
│   ├── CHANGELOG.md           # سجل التعديلات التاريخي
│   ├── KNOWN_ISSUES.md        # المشكلات والتحديات الهندسية المفتوحة
│   └── NEXT_TASK.md           # المهمة التالية المحددة بدقة
├── app/                       # وحدة التطبيق الأساسية (Android Application Module)
│   ├── build.gradle.kts       # إعدادات البناء (namespace & applicationId: com.xfiles.pro)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/xfiles/pro/
│       │   └── res/           # الموارد: سلاسل النصوص للغات الأربع والأيقونات
│       │       ├── values/strings.xml     # المرجع الإنجليزي (en)
│       │       ├── values-ar/strings.xml  # المرجع العربي (ar)
│       │       ├── values-es/strings.xml  # المرجع الإسباني (es)
│       │       └── values-fa/strings.xml  # المرجع الفارسي (fa)
│       ├── test/java/com/xfiles/pro/      # اختبارات الوحدة وRobolectric (27 اختباراً)
│       └── androidTest/java/com/xfiles/pro/
├── gradle/
│   └── libs.versions.toml     # كتالوج إصدارات المكتبات الموحد
├── build.gradle.kts           # إعدادات البناء على مستوى الجذر
├── settings.gradle.kts        # تعريف المشروع والوحدات (rootProject.name = "XFiles Pro")
└── metadata.json              # بيانات تعريف التطبيق على منصة Google AI Studio
```

---

## 2. الهيكل الفعلي لحزم الكود المصدرية (`app/src/main/java/com/xfiles/pro/`)
```text
com.xfiles.pro
│
├── XFilesApplication.kt                 # فئة التطبيق الرئيسية المهيئة لـ AppContainer
├── MainActivity.kt                      # نقطة الدخول الداعمة للغات والتبديل الفوري
│
├── core/                                # المكونات المشتركة
│   ├── dispatcher/
│   │   └── DispatchersProvider.kt       # واجهة وتطبيق موجهات خيوط الكوروتين
│   ├── result/
│   │   └── ResultWrapper.kt             # معالجة النتائج (Success, Error, Loading)
│   └── util/
│       ├── ByteFormatter.kt             # تنسيق أحجام الملفات (B, KB, MB, GB, TB)
│       └── FileExtensions.kt            # اكتشاف الامتدادات وتصنيف أنواع الملفات
│
├── domain/                              # طبقة منطق الأعمال والنماذج
│   ├── model/
│   │   ├── AppLanguage.kt               # نموذج اللغات الأربع وخصائص الـ RTL/LTR
│   │   ├── FileItem.kt                  # نموذج عنصر الملف أو المجلد
│   │   ├── FileType.kt                  # تصنيفات أنواع الملفات
│   │   ├── StorageVolume.kt             # نموذج وحدة التخزين وحساب السعات
│   │   └── FileSortOption.kt            # معايير فرز الملفات
│   └── repository/
│       ├── LanguageRepository.kt        # عقد مستودع إدارة اللغات
│       └── StorageRepository.kt         # عقد مستودع التخزين
│
├── data/                                # طبقة البيانات
│   ├── local/
│   │   ├── database/
│   │   │   ├── XFilesDatabase.kt        # قاعدة بيانات Room التأسيسية
│   │   │   ├── entity/IndexedFileEntity.kt
│   │   │   └── dao/IndexedFileDao.kt
│   │   └── datastore/
│   │       └── LanguagePreferences.kt   # حفظ تفضيلات اللغة وحالة أول تشغيل عبر DataStore
│   └── repository/
│       ├── LanguageRepositoryImpl.kt    # تطبيق مستودع اللغة المدعوم بـ DataStore
│       └── StorageRepositoryImpl.kt     # تطبيق مستودع التخزين المعتمد على SAF
│
├── di/                                  # حاوية حقن التبعيات
│   └── AppContainer.kt                  # توفير Singletons للمستودعات وقاعدة البيانات واللغات
│
└── ui/                                  # طبقة واجهة المستخدم
    ├── language/
    │   ├── LanguageUiState.kt           # حالة واجهة اللغة
    │   ├── LanguageViewModel.kt         # إدارة أحداث وحالة تفضيلات اللغة
    │   ├── LanguageSelectionScreen.kt   # شاشة اختيار اللغة عند أول تشغيل
    │   ├── LanguagePickerDialog.kt      # النافذة الحوارية لتغيير اللغة الفوري أثناء التشغيل
    │   └── LocalizedApp.kt              # مغلف Compose الديناميكي لحقن السياق و Direction
    └── theme/
        ├── Color.kt
        ├── Type.kt
        └── Theme.kt                     # سمة XFilesTheme الأساسية
```

---

## 3. حزم الاختبارات (`app/src/test/java/com/xfiles/pro/`) — 29 اختباراً ناجحاً
```text
com.xfiles.pro
├── ExampleUnitTest.kt                   # اختبار عام (1)
├── ExampleRobolectricTest.kt            # اختبار Robolectric لاستخراج اسم التطبيق (1)
├── core/
│   ├── ByteFormatterTest.kt             # اختبارات دقة تحويل البايتات (3)
│   └── ResultWrapperTest.kt             # اختبارات حالات النتيجة (3)
├── domain/
│   ├── AppLanguageTest.kt               # اختبارات خصائص اللغات واتجاهات RTL/LTR و fromCode (5)
│   └── FileItemTest.kt                  # اختبارات تصنيف الملفات (2)
├── data/
│   ├── LanguageRepositoryTest.kt        # اختبارات تدفق وحفظ اللغة وأول تشغيل (4)
│   └── RealDataStoreLanguageTest.kt     # اختبارات فعلية على قرص DataStore واستعادة البيانات عبر نسخ جديدة (2)
└── ui/
    ├── LanguageViewModelTest.kt         # اختبارات إدارة وتدفق حالة الـ ViewModel (4)
    └── RobolectricLocalizationTest.kt   # اختبارات Robolectric لحل النصوص باللغات الأربع (4)
```
