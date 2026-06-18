package xyz.refrain.onlineedu.config;

import java.util.Arrays;
import java.util.List;

/**
 * Computer Science and Technology (Sino-foreign cooperative education)
 * curriculum catalog based on the 2022 training plan.
 */
final class CurriculumCourseCatalog {

    static final String MAJOR_NAME = "计算机科学与技术（中外合作办学）";

    private static final String PUBLIC_REQUIRED = "通识教育必修";
    private static final String GENERAL_ELECTIVE = "通识教育选修";
    private static final String DISCIPLINE_REQUIRED = "学科基础";
    private static final String MAJOR_REQUIRED = "专业发展必修";
    private static final String MAJOR_ELECTIVE = "专业发展选修";
    private static final String PRACTICE_REQUIRED = "综合实践";

    private static final String CIVICS = "/api/pub/image/demo/curriculum/civics-law.jpg";
    private static final String MILITARY = "/api/pub/image/demo/curriculum/military-training.jpg";
    private static final String SPORTS = "/api/pub/image/demo/curriculum/physical-education.jpg";
    private static final String ENGLISH = "/api/pub/image/demo/curriculum/academic-english.jpg";
    private static final String CAREER = "/api/pub/image/demo/curriculum/career-entrepreneurship.jpg";
    private static final String MENTAL_HEALTH = "/api/pub/image/demo/curriculum/mental-health.jpg";
    private static final String HUMANITIES = "/api/pub/image/demo/curriculum/public-art-humanities.jpg";
    private static final String MATH = "/api/pub/image/demo/curriculum/mathematics.jpg";
    private static final String PHYSICS = "/api/pub/image/demo/curriculum/physics-electronics-lab.jpg";
    private static final String PROGRAMMING = "/api/pub/image/demo/course-cover-01.jpg";
    private static final String DATA_STRUCTURE = "/api/pub/image/demo/course-cover-02.jpg";
    private static final String OPERATING_SYSTEM = "/api/pub/image/demo/course-cover-03.jpg";
    private static final String NETWORK = "/api/pub/image/demo/course-cover-04.jpg";
    private static final String DATABASE = "/api/pub/image/demo/course-cover-05.jpg";
    private static final String AI = "/api/pub/image/demo/course-cover-06.jpg";
    private static final String SOFTWARE = "/api/pub/image/demo/curriculum/software-project.jpg";
    private static final String DATA_ANALYSIS = "/api/pub/image/demo/curriculum/data-analysis.jpg";
    private static final String COMPUTER_ARCHITECTURE = "/api/pub/image/demo/course-cover-10.jpg";
    private static final String ROBOTICS = "/api/pub/image/demo/course-cover-12.jpg";
    private static final String GENOMICS = "/api/pub/image/demo/curriculum/genomics-medical-data.jpg";
    private static final String DISTRIBUTED = "/api/pub/image/demo/curriculum/distributed-systems.jpg";
    private static final String BLOCKCHAIN = "/api/pub/image/demo/curriculum/blockchain-economy.jpg";
    private static final String GRADUATION = "/api/pub/image/demo/curriculum/graduation-defense.jpg";

    private CurriculumCourseCatalog() {
    }

    static List<CourseSeed> all() {
        return Arrays.asList(
                c("32111043", "思想道德与法治", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 3, 52, "1/2", "考试", "", CIVICS, "13800138004"),
                c("32110986", "中国近现代史纲要", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 3, 52, "1/2", "考试", "", CIVICS, "13800138004"),
                c("32111044", "马克思主义基本原理", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 3, 52, "2/3", "考试", "", CIVICS, "13800138004"),
                c("32111045", "毛泽东思想和中国特色社会主义理论体系概论", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 3, 52, "3", "考试", "", CIVICS, "13800138004"),
                c("32111011", "习近平新时代中国特色社会主义思想概论", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 3, 52, "4", "考试", "", CIVICS, "13800138004"),
                c("21110001", "形势与政策", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2, 64, "1-8", "考查", "", CIVICS, "13800138004"),
                c("91110001", "军事理论", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2, 32, "1", "考查", "", MILITARY, "13800138004"),
                c("91110002", "军事技能", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2, 48, "1", "考查", "", MILITARY, "13800138004"),
                c("", "体育 A", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 0.5, 28, "1", "考试", "培养方案未列课程代码", SPORTS, "13800138004"),
                c("", "体育 B", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 0.5, 28, "2", "考试", "培养方案未列课程代码", SPORTS, "13800138004"),
                c("", "体育 C", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 1, 28, "3", "考试", "培养方案未列课程代码", SPORTS, "13800138004"),
                c("", "体育 D", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 1, 28, "4", "考试", "培养方案未列课程代码", SPORTS, "13800138004"),
                c("07113478", "体育 E", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 0.5, 6, "5/6", "考查", "", SPORTS, "13800138004"),
                c("07113479", "体育 F", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 0.5, 6, "7/8", "考查", "", SPORTS, "13800138004"),
                c("21116434", "学术语言与沟通 I", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2.5, 88, "1", "考试", "", ENGLISH, "13800138004"),
                c("21116435", "学术语言与沟通 II", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2.5, 88, "2", "考试", "", ENGLISH, "13800138004"),
                c("21116436", "学术语言与研究方法 I", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2.5, 96, "3", "考试", "", ENGLISH, "13800138004"),
                c("21116437", "学术语言与研究方法 II", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 2.5, 96, "4", "考试", "", ENGLISH, "13800138004"),
                c("90110031", "大学生职业发展与就业指导 A", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 0.5, 8, "2", "考查", "", CAREER, "13800138004"),
                c("90110032", "大学生职业发展与就业指导 B", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 0.5, 8, "5", "考试", "", CAREER, "13800138004"),
                c("21110007", "大学生创业基础", PUBLIC_REQUIRED, "PUBLIC_REQUIRED", 1, 16, "2", "考试", "", CAREER, "13800138004"),

                c("", "通识选修：思想政治类选课组", GENERAL_ELECTIVE, "GENERAL_ELECTIVE", 2, 32, "1-8", "考查", "培养方案只规定类别与最低学分，具体课程由学校通识课库提供", CIVICS, "13800138004"),
                c("", "通识选修：心理健康类选课组", GENERAL_ELECTIVE, "GENERAL_ELECTIVE", 2, 32, "1-8", "考查", "培养方案只规定类别与最低学分，具体课程由学校通识课库提供", MENTAL_HEALTH, "13800138004"),
                c("", "通识选修：公共艺术类选课组", GENERAL_ELECTIVE, "GENERAL_ELECTIVE", 2, 32, "1-8", "考查", "培养方案只规定类别与最低学分，具体课程由学校通识课库提供", HUMANITIES, "13800138004"),
                c("", "通识选修：人文社科类选课组", GENERAL_ELECTIVE, "GENERAL_ELECTIVE", 2, 32, "1-8", "考查", "培养方案只规定类别与最低学分，具体课程由学校通识课库提供", HUMANITIES, "13800138004"),

                c("21210141", "高等数学 Ⅰ A", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 4, 64, "1", "考试", "", MATH, "13800138002"),
                c("21210142", "高等数学 Ⅰ B", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 5, 80, "2", "考试", "", MATH, "13800138002"),
                c("21216039", "高级语言程序设计（C 语言）", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 2, 48, "2", "考试", "", PROGRAMMING, "13800138002"),
                c("15212258", "大学物理Ⅰ", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 6, 96, "2", "考试", "", PHYSICS, "13800138002"),
                c("15210061", "大学物理实验", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 1.5, 36, "3", "考试", "", PHYSICS, "13800138002"),
                c("21210200", "线性代数 Ⅰ", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 3, 48, "3", "考试", "", MATH, "13800138002"),
                c("212136601", "数据结构", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 4, 72, "3", "考试", "", DATA_STRUCTURE, "13800138002"),
                c("21216047", "数据分析方法", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 4, 72, "4", "考试", "", DATA_ANALYSIS, "13800138003"),
                c("21210700", "数据库原理及应用", DISCIPLINE_REQUIRED, "DISCIPLINE_REQUIRED", 4, 72, "4", "考试", "", DATABASE, "13800138003"),

                c("21215702", "专业导论与个人发展管理", MAJOR_REQUIRED, "MAJOR_REQUIRED", 0.5, 8, "1", "考查", "", CAREER, "13800138002"),
                c("21316043", "离散数学", MAJOR_REQUIRED, "MAJOR_REQUIRED", 4, 64, "2", "考试", "", MATH, "13800138002"),
                c("21316048", "概率论及其应用", MAJOR_REQUIRED, "MAJOR_REQUIRED", 3, 48, "3", "考试", "", MATH, "13800138002"),
                c("21316049", "统计学", MAJOR_REQUIRED, "MAJOR_REQUIRED", 3, 48, "3", "考试", "", DATA_ANALYSIS, "13800138003"),
                c("21316304", "数字电路", MAJOR_REQUIRED, "MAJOR_REQUIRED", 4, 72, "4", "考试", "", PHYSICS, "13800138002"),
                c("21326160", "计算机网络", MAJOR_REQUIRED, "MAJOR_REQUIRED", 4, 72, "4", "考试", "", NETWORK, "13800138003"),
                c("21325926", "算法分析与设计", MAJOR_REQUIRED, "MAJOR_REQUIRED", 3, 56, "5", "考试", "", DATA_STRUCTURE, "13800138002"),
                c("21326064", "计算机组成原理", MAJOR_REQUIRED, "MAJOR_REQUIRED", 4, 72, "5", "考试", "", COMPUTER_ARCHITECTURE, "13800138002"),
                c("21316053", "操作系统原理", MAJOR_REQUIRED, "MAJOR_REQUIRED", 4, 72, "5", "考试", "", OPERATING_SYSTEM, "13800138002"),
                c("21316313", "编译原理", MAJOR_REQUIRED, "MAJOR_REQUIRED", 3, 56, "6", "考试", "", SOFTWARE, "13800138002"),

                c("212101502", "计算机科学导论", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "1", "考试", "必选课程", PROGRAMMING, "13800138002"),
                c("21316372", "编程导论（Python）", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "1", "考查", "必选课程", PROGRAMMING, "13800138002"),
                c("21326464", "Web 开发技术", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "2", "考试", "", SOFTWARE, "13800138002"),
                c("21326465", "面向对象程序设计语言（C++）", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "3", "考试", "必选课程", PROGRAMMING, "13800138002"),
                c("21326466", "Linux 系统编程", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 56, "3", "考试", "", OPERATING_SYSTEM, "13800138002"),
                c("21326467", "Java 语言程序设计", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "4", "考试", "", PROGRAMMING, "13800138002"),
                c("21326452", "软件工程", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 56, "5", "考试", "必选课程", SOFTWARE, "13800138002"),
                c("213220002", "汇编语言程序设计", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 56, "5/6", "考试", "", COMPUTER_ARCHITECTURE, "13800138002"),
                c("21326468", "Windows 程序设计（API）", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "5/6", "考查", "", PROGRAMMING, "13800138002"),
                c("21326469", "移动平台应用程序开发", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "5/6", "考查", "", SOFTWARE, "13800138002"),
                c("21216221", "数据科学导论", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2, 48, "2", "考查", "必选课程", DATA_ANALYSIS, "13800138003"),
                c("21326470", "数值分析方法", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 48, "4", "考试", "", MATH, "13800138003"),
                c("21326471", "NoSQL 数据库", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 56, "4", "考查", "", DATABASE, "13800138003"),
                c("21326472", "基因组学数据的统计分析和挖掘", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "5", "考查", "", GENOMICS, "13800138003"),
                c("21326473", "医疗大数据统计学", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "5", "考查", "", GENOMICS, "13800138003"),
                c("21326474", "大数据可视化技术", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "5", "考查", "", DATA_ANALYSIS, "13800138003"),
                c("21326367", "机器学习", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 56, "5", "考查", "", AI, "13800138003"),
                c("21316107", "人工智能", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 3, 56, "6", "考试", "外教课程，必选课程", AI, "13800138003"),
                c("21326475", "数字经济与区块链技术", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "5/6", "考查", "", BLOCKCHAIN, "13800138003"),
                c("21326476", "机器视觉与行业大数据应用", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "5/6", "考查", "", ROBOTICS, "13800138003"),
                c("21326477", "智能系统与行业大数据应用", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 2.5, 44, "5/6", "考查", "", AI, "13800138003"),
                c("21326342", "实验室安全教育", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 0.5, 8, "5/6", "考查", "必选课程", PHYSICS, "13800138003"),
                c("21326478", "工程伦理与职业道德", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 0.5, 8, "5/6", "考查", "必选课程", CIVICS, "13800138003"),
                c("21116438", "学术文化与科研能力 I", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 0.5, 24, "5", "考查", "出国学生必修", ENGLISH, "13800138004"),
                c("21116439", "学术文化与科研能力 II", MAJOR_ELECTIVE, "MAJOR_ELECTIVE", 0.5, 24, "6", "考查", "出国学生必修", ENGLISH, "13800138004"),

                c("21616411", "程序设计综合实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 2, 48, "2", "考查", "", SOFTWARE, "13800138002"),
                c("21616480", "数据挖掘应用实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 2.5, 60, "4", "考查", "外教课程", DATA_ANALYSIS, "13800138003"),
                c("21616479", "数据结构与算法综合实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 2, 48, "4", "考查", "", DATA_STRUCTURE, "13800138002"),
                c("21616481", "统计学应用实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 2.5, 60, "5", "考查", "外教课程", DATA_ANALYSIS, "13800138003"),
                c("21616482", "大规模分布式系统综合实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 2.5, 60, "5", "考查", "", DISTRIBUTED, "13800138003"),
                c("21614292", "数据科学行业应用综合实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 2.5, 60, "6", "考查", "", DATA_ANALYSIS, "13800138003"),
                c("21616413", "工程职业实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 1, 24, "6", "考查", "", CAREER, "13800138002"),
                c("21616484", "毕业实习", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 8, 192, "7", "考查", "", GRADUATION, "13800138002"),
                c("21616483", "毕业论文（设计、作品）", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 8, 192, "7-8", "考查", "", GRADUATION, "13800138002"),
                c("21616463", "劳动教育与社会实践", PRACTICE_REQUIRED, "PRACTICE_REQUIRED", 1, 24, "8", "考查", "", CAREER, "13800138004")
        );
    }

    private static CourseSeed c(String code, String title, String category, String courseType, double credit,
                                int lessonNum, String curriculumSemester, String assessmentMethod, String remarks,
                                String cover, String teacherMobile) {
        String seededRemarks = "培养方案课程";
        if (remarks != null && !remarks.isEmpty()) {
            seededRemarks += "；" + remarks;
        }
        return new CourseSeed(code, title, category, courseType, credit, lessonNum, curriculumSemester,
                assessmentMethod, seededRemarks, uniqueCover(code, title), teacherMobile);
    }

    private static String uniqueCover(String code, String title) {
        String identity = code + "|" + title;
        return "/api/pub/image/demo/curriculum/unique/" + Integer.toHexString(identity.hashCode()) + ".jpg";
    }

    static final class CourseSeed {
        final String code;
        final String title;
        final String category;
        final String courseType;
        final double credit;
        final int lessonNum;
        final String curriculumSemester;
        final String assessmentMethod;
        final String remarks;
        final String cover;
        final String teacherMobile;

        private CourseSeed(String code, String title, String category, String courseType, double credit, int lessonNum,
                           String curriculumSemester, String assessmentMethod, String remarks, String cover,
                           String teacherMobile) {
            this.code = code;
            this.title = title;
            this.category = category;
            this.courseType = courseType;
            this.credit = credit;
            this.lessonNum = lessonNum;
            this.curriculumSemester = curriculumSemester;
            this.assessmentMethod = assessmentMethod;
            this.remarks = remarks;
            this.cover = cover;
            this.teacherMobile = teacherMobile;
        }
    }
}
