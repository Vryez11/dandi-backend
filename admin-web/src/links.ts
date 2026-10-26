/**
 * 어드민 첫 화면의 운영 도구 바로가기. GA4·Firebase는 Google 로그인과 그 프로젝트 권한이 있어야 열린다.
 * 링크의 속성 id·프로젝트 id는 비밀값이 아니다(권한이 없으면 열리지 않는다).
 */
export type ToolLink = {
  label: string;
  description: string;
  url: string;
};

export type ToolGroup = {
  title: string;
  tool: string;
  links: ToolLink[];
  /** 링크 대신 먼저 해야 할 일(터널 열기 등)이 있으면 보여 준다. */
  command?: { description: string; value: string };
};

const GA4 = "https://analytics.google.com/analytics/web/?authuser=0&hl=ko#/a250284629p551192354/reports/explorer";
const FIREBASE = "https://console.firebase.google.com/u/0/project/dandi-swm";
const ANDROID_APP = "app/android:com.dandi.nyummy";

export const TOOL_GROUPS: ToolGroup[] = [
  {
    title: "사용자 행동",
    tool: "GA4",
    links: [
      {
        label: "이벤트 지표",
        description: "어떤 이벤트가 얼마나 일어나는지",
        url: `${GA4}?params=_u..nav%3Dmaui&r=top-events&ruid=top-events,business-objectives,examine-user-behavior&collectionId=business-objectives`,
      },
      {
        label: "화면 방문율",
        description: "화면별 방문 수와 비율",
        url: `${GA4}?params=_u..nav%3Dmaui&r=all-pages-and-screens&ruid=all-pages-and-screens,business-objectives,examine-user-behavior&collectionId=business-objectives`,
      },
    ],
  },
  {
    title: "앱 안정성",
    tool: "Firebase Crashlytics",
    links: [
      {
        label: "앱 크래시 추적",
        description: "Android · 최근 7일 · 열린 크래시 이슈(많이 난 순)",
        url: `${FIREBASE}/crashlytics/${ANDROID_APP}/issues?hl=ko&time=7d&state=open&types=crash&tag=all&sort=eventCount`,
      },
    ],
  },
  {
    title: "앱 성능",
    tool: "Firebase Performance",
    links: [
      {
        // 버전 필터를 걸지 않아 모든 버전 추이를 본다. 버전은 콘솔에서 고른다.
        label: "앱 성능 추이",
        description: "TTI(첫 화면을 쓸 수 있게 되기까지) · 이미지 렌더링 성능 · 최근 24시간",
        url: `${FIREBASE}/performance/${ANDROID_APP}/trends?hl=ko&time=24h`,
      },
    ],
  },
  {
    title: "데이터",
    tool: "Metabase",
    links: [
      {
        label: "Metabase 열기",
        description: "SWM 지표 대시보드 · 아래 터널을 먼저 열어야 열려요",
        url: "http://localhost:13000",
      },
    ],
    command: {
      description: "Metabase는 인터넷에 열려 있지 않아요. 백엔드 레포 루트에서 터널을 열고(Ctrl+C로 닫음) 위 링크를 누르세요.",
      value: "./scripts/tunnel.sh metabase",
    },
  },
];
