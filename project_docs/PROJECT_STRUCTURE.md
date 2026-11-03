# XFiles Pro — PROJECT STRUCTURE (بنية وهيكل مجلدات المشروع)

يشرح هذا المستند التقسيم الهيكلي لحزم المشروع وملفات المصدر والموارد بعد اكتمال المراحل 1 و 2 و 3 و 4 و 5A و 5B و 6 و 7.

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
│       ├── test/java/com/xfiles/pro/      # اختبارات الوحدة وRobolectric (163 اختباراً ناجحاً 100%)
│       └── androidTest/java/com/xfiles/pro/
├── gradle/
│   └── libs.versions.toml     # كتالوج إصدارات المكتبات الموحد
├── build.gradle.kts           # إعدادات البناء على مستوى الجذر
├── settings.gradle.kts        # تعريف المشروع والوحدات (rootProject.name = "XFiles Pro")
├── .env.example               # نموذج المتغيرات البيئية الافتراضية للـ Secrets Plugin
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
│   │   ├── ThemeMode.kt                 # نموذج السمات الأربع (Classic, Midnight, Royal, System)
│   │   ├── FileItem.kt                  # نموذج عنصر الملف أو المجلد
│   │   ├── FileType.kt                  # تصنيفات أنواع الملفات
│   │   ├── StorageVolume.kt             # نموذج وحدة التخزين وحساب السعات
│   │   ├── FileSortOption.kt            # معايير فرز الملفات
│   │   ├── FileConflictResolution.kt    # خيارات تعارض الأسماء (OVERWRITE, SKIP, AUTO_RENAME)
│   │   ├── SafPermission.kt             # نموذج أذونات SAF المحفوظة
│   │   ├── StorageEvent.kt              # أحداث التخزين اللحظية (Created, Updated, Deleted, Moved)
│   │   ├── IndexingState.kt             # نموذج حالة الفهرسة الدائمة والتقدم
│   │   ├── FileTab.kt                   # نموذج التبويب المستقل للتصفح المتعدد
│   │   ├── PaneId.kt                    # معرّف الألواح للـ Dual Pane (PRIMARY, SECONDARY)
│   │   ├── SearchFilterCriteria.kt      # معايير البحث المتقدم المركبة
│   │   ├── SizeFilterRange.kt           # نطاقات تصفية الأحجام
│   │   ├── DateFilterRange.kt           # نطاقات تصفية التواريخ
│   │   ├── SearchTarget.kt              # تحديد أهداف البحث (ملفات/مجلدات/الكل)
│   │   ├── ArchiveFormat.kt             # صيغ الأرشيفات المدعومة (ZIP, TAR, TAR.GZ, TGZ, GZIP, BZIP2, XZ, 7Z, RAR)
│   │   ├── CompressionLevel.kt          # مستويات الضغط (STORE, FAST, NORMAL, MAXIMUM, ULTRA)
│   │   ├── ArchiveEntryItem.kt          # تمثيل عناصر الأرشيف الافتراضية والمسارات والأحجام
│   │   ├── ArchiveProgress.kt           # تقدم عمليات الأرشيف اللحظية
│   │   ├── ArchiveSecurityPolicy.kt     # فحص سياسات الأمان (Zip Slip & Archive Bombs)
│   │   └── ArchiveSecurityException.kt  # استثناءات الأمان للأرشيفات
│   └── repository/
│       ├── LanguageRepository.kt        # عقد مستودع إدارة اللغات
│       ├── ThemeRepository.kt           # عقد مستودع إدارة السمات
│       ├── StorageRepository.kt         # عقد مستودع التخزين والـ SAF ودفق الأحداث
│       ├── FileIndexingRepository.kt    # عقد مستودع الفهرسة الدائمة والبحث النصي FTS
│       ├── TabRepository.kt             # عقد مستودع التبويبات المتعددة الدائمة
│       ├── SearchHistoryRepository.kt   # عقد مستودع سجل البحث
│       └── ArchiveRepository.kt         # عقد مستودع إدارة وضغط وفك واستعراض الأرشيفات
│
├── data/                                # طبقة البيانات
│   ├── local/
│   │   ├── database/
│   │   │   ├── XFilesDatabase.kt        # قاعدة بيانات Room (إصدار 2 مع MIGRATION_1_2)
│   │   │   ├── entity/IndexedFileEntity.kt     # جدول الفهرس الدائم الموسع بـ 16 حقلاً
│   │   │   ├── entity/IndexedFileFtsEntity.kt  # جدول FTS4/FTS5 الافتراضي للبحث النصي
│   │   │   └── dao/IndexedFileDao.kt           # استعلامات FTS والبحث والتصفية الدفعية
│   │   ├── datastore/
│   │   │   ├── LanguagePreferences.kt   # حفظ تفضيلات اللغة وحالة أول تشغيل عبر DataStore
│   │   │   ├── ThemePreferences.kt      # حفظ تفضيلات السمة ومزامنتها عبر نفس الـ DataStore
│   │   │   ├── TabPreferences.kt        # حفظ التبويبات المتعددة النشطة والدائمة
│   │   │   └── SearchPreferences.kt     # استمرارية سجل آخر 15 استعلام بحث
│   │   ├── archive/                     # محركات الأرشيفات المتخصصة
│   │   │   ├── ArchiveBufferConstants.kt # ثوابت تدفق الذاكرة ومصفوفات الضغط
│   │   │   ├── ZipArchiveHandler.kt     # معالجة ZIP وتشفير AES-256 والتعديل الموضعي (Zip4j)
│   │   │   ├── TarArchiveHandler.kt     # معالجة TAR و TAR.GZ و TGZ (Commons Compress)
│   │   │   ├── SingleCompressorHandler.kt # معالجة تدفقات GZIP و BZIP2 و XZ
│   │   │   ├── SevenZArchiveHandler.kt  # إنشاء واستخراج واستعراض أرشيفات 7Z (Commons Compress)
│   │   │   └── RarArchiveHandler.kt     # استعراض واستخراج أرشيفات RAR (Junrar)
│   │   └── storage/
│   │       ├── SafManager.kt            # إدارة أذونات ومستندات Storage Access Framework
│   │       └── MediaStoreManager.kt     # إدارة استعلامات Android MediaStore
│   ├── worker/
│   │   └── FileIndexingWorker.kt        # عامل WorkManager للجدولة الخلفية مع قيود البطارية
│   └── repository/
│       ├── LanguageRepositoryImpl.kt    # تطبيق مستودع اللغة المدعوم بـ DataStore
│       ├── ThemeRepositoryImpl.kt       # تطبيق مستودع السمة المدعوم بـ DataStore
│       ├── StorageRepositoryImpl.kt     # تطبيق مستودع التخزين المعتمد على SAF و MediaStore والنسخ الآمن
│       ├── FileIndexingRepositoryImpl.kt # تطبيق مستودع الفهرسة التزايدي والبحث الفوري FTS
│       ├── TabRepositoryImpl.kt         # تطبيق مستودع التبويبات المتعددة
│       ├── SearchHistoryRepositoryImpl.kt # تطبيق مستودع سجل البحث
│       └── ArchiveRepositoryImpl.kt     # تطبيق مستودع الأرشيفات المركزي وسياسات الأمان
│
├── di/                                  # حاوية حقن التبعيات
│   └── AppContainer.kt                  # توفير Singletons للمستودعات وقاعدة البيانات والـ SAF والأرشيفات
│
└── ui/                                  # طبقة واجهة المستخدم
    ├── navigation/
    │   ├── NavDestination.kt            # تعداد الوجهات الخمس (HOME, FILES, MEDIA, TOOLS, SETTINGS)
    │   └── XFilesAppShell.kt            # الهيكل التكيفي للتنقل (NavigationBar للهواتف / NavigationRail للشاشات العريضة)
    ├── components/
    │   └── XFilesTopAppBar.kt           # الشريط العلوي التفاعلي الموحد لجميع الشاشات
    ├── home/
    │   └── HomeScreen.kt                # الشاشة الرئيسية، بطاقة التخزين الحقيقية، شبكة الوصول السريع، والملفات الأخيرة
    ├── files/
    │   ├── FilesUiState.kt              # حالة واجهة استعراض الملفات والعمليات
    │   ├── FilesViewModel.kt            # إدارة منطق التصفح، شريط المسار، وعمليات الملفات
    │   ├── FilesScreen.kt               # شاشة استعراض الملفات، شريط المسار التفاعلي، زر FAB، ونوافذ العمليات
    │   └── components/                  # مكونات التصفح المتقدم
    │       ├── DualPaneFilesView.kt     # واجهة اللوح المزدوج للشاشات اللوحية والعمليات البينية
    │       ├── FileTabBar.kt            # شريط التبويبات المتعددة المتجاوب
    │       ├── AdvancedSearchSheet.kt   # ورقة الفلاتر المتقدمة المركبة
    │       └── SearchHistoryView.kt     # استعراض وحذف سجل البحث
    ├── archive/                         # واجهات إدارة واستعراض الأرشيفات
    │   ├── ArchiveUiState.kt            # حالة شاشة استعراض وفك الأرشيف
    │   ├── ArchiveViewModel.kt          # إدارة تصفح الأرشيف الشجري والفك والحذف والبحث الداخلي
    │   ├── ArchiveViewerScreen.kt       # شاشة استعراض الأرشيف والشجرة الافتراضية
    │   └── components/
    │       ├── CreateArchiveDialog.kt   # نافذة إنشاء الأرشيف واختيار الصيغة والضغط وكلمة المرور
    │       ├── ExtractArchiveDialog.kt  # نافذة فك الضغط ومجلد الوجهة والتعارض
    │       ├── PasswordPromptDialog.kt  # نافذة طلب كلمة المرور للأرشيفات المشفرة
    │       └── ArchiveProgressDialog.kt # نافذة نسبة التقدم والإلغاء الفوري
    ├── media/
    │   └── MediaScreen.kt               # شاشة الوسائط مع تبويبات الصور والفيديوهات والصوتيات
    ├── tools/
    │   └── ToolsScreen.kt               # شاشة الأدوات المتقدمة المخططة (المحلل، المنظف، السلة، الخزنة، والمشاركة)
    ├── settings/
    │   └── SettingsScreen.kt            # شاشة الإعدادات المدمجة مع اللغات والسمات وتفاصيل المعمارية
    ├── language/
    │   ├── LanguageUiState.kt           # حالة واجهة اللغة
    │   ├── LanguageViewModel.kt         # إدارة أحداث وحالة تفضيلات اللغة
    │   ├── LanguageSelectionScreen.kt   # شاشة اختيار اللغة عند أول تشغيل
    │   ├── LanguagePickerDialog.kt      # النافذة الحوارية لتغيير اللغة الفوري أثناء التشغيل
    │   └── LocalizedApp.kt              # مغلف Compose الديناميكي لحقن السياق و Direction
    └── theme/
        ├── Color.kt                     # تعريفات ألوان ColorScheme للسمات الثلاث
        ├── ColorTokens.kt               # نظام Color Tokens المركزي (13 رمزاً لونياً)
        ├── ThemeMode.kt                 # تمثيل السمة
        ├── ThemeUiState.kt              # حالة واجهة السمة
        ├── ThemeViewModel.kt            # إدارة تفضيلات السمة والتبديل الفوري
        ├── ThemePickerDialog.kt         # النافذة الحوارية لاختيار السمة ومعاينة ألوانها
        ├── Type.kt                      # خطوط وأنماط النصوص
        └── Theme.kt                     # سمة XFilesTheme المركزية المطبقة للسمات الأربع
```

---

## 3. حزم الاختبارات (`app/src/test/java/com/xfiles/pro/`) — 86 اختباراً ناجحاً بنسبة 100%
```text
com.xfiles.pro
├── ExampleUnitTest.kt                           # اختبار عام (1)
├── ExampleRobolectricTest.kt                    # اختبار Robolectric لاستخراج اسم التطبيق (1)
├── core/
│   ├── ByteFormatterTest.kt                     # اختبارات دقة تحويل البايتات (3)
│   └── ResultWrapperTest.kt                     # اختبارات حالات النتيجة (3)
├── domain/
│   ├── AppLanguageTest.kt                       # اختبارات خصائص اللغات واتجاهات RTL/LTR و fromCode (5)
│   ├── ThemeModeTest.kt                         # اختبارات خصائص السمات والافتراضية والتعرف من المعرفات (5)
│   └── FileItemTest.kt                          # اختبارات تصنيف الملفات (2)
├── data/
│   ├── LanguageRepositoryTest.kt                # اختبارات تدفق وحفظ اللغة وأول تشغيل (4)
│   ├── ThemeRepositoryTest.kt                   # اختبارات تدفق وحفظ السمات الأربع (3)
│   ├── RealDataStoreLanguageTest.kt             # اختبارات قرص DataStore لاستعادة اللغة (2)
│   ├── RealDataStoreThemeTest.kt                # اختبارات قرص DataStore لاستعادة السمة والتزامن مع اللغة (4)
│   ├── StorageOperationTest.kt                  # اختبارات عمليات الملفات والـ SAF والأسماء العربية والنسخ الآمن (11)
│   └── FileIndexingEngineTest.kt                # اختبارات Room FTS والبحث العربي والتزايدي والمزامنة اللحظية (15)
└── ui/
    ├── LanguageViewModelTest.kt                 # اختبارات إدارة وتدفق حالة لغة ViewModel (4)
    ├── RobolectricLocalizationTest.kt           # اختبارات حل النصوص باللغات الأربع (4)
    ├── theme/
    │   ├── ColorTokensTest.kt                   # اختبارات صحة وتطابق الرموز اللونية الـ 13 لكل سمة (5)
    │   ├── SystemThemeDynamicResponseTest.kt    # اختبارات استجابة سمة النظام لنمط أندرويد الداكن (2)
    │   ├── ThemeViewModelTest.kt                # اختبارات إدارة وتدفق واختيار السمة في ViewModel (6)
    │   └── ThemeAndLocalizationCombinationsTest.kt # اختبارات فحص التركيبات الـ 16 كاملة (4)
    └── navigation/
        ├── NavDestinationTest.kt                # اختبارات صحة واستخراج وتفرد الوجهات الخمس (5)
        ├── NavigationAndShellRobolectricTest.kt # اختبارات Robolectric للهيكل التنقلي، واللغات، والسمات، والـ RTL (7)
        └── NavigationStateAndBackHandlerTest.kt # اختبارات إدارة حالات التنقل، زر الرجوع، وأوضاع العرض (5)
```
