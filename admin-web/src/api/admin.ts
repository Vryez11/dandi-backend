import { request } from "./client";

const PREFIX = "/admin/api";

export type AdminResponse = { adminId: number; email: string; name: string };

export type InquiryCategory = "BUG_REPORT" | "QUESTION" | "SUGGESTION";

export const INQUIRY_CATEGORY_LABELS: Record<InquiryCategory, string> = {
  BUG_REPORT: "버그 신고",
  QUESTION: "질문",
  SUGGESTION: "제안",
};

export type AdminInquirySummary = {
  inquiryId: number;
  userId: number;
  category: InquiryCategory;
  questionTitle: string;
  createdAt: string;
  isAnswered: boolean;
  answeredAt: string | null;
};

export type AdminInquiries = {
  inquiries: AdminInquirySummary[];
  nextCursor: number | null;
  hasNext: boolean;
};

export type AdminInquiry = {
  inquiryId: number;
  userId: number;
  category: InquiryCategory;
  questionTitle: string;
  questionContent: string;
  imageUrl: string | null;
  appVersion: string | null;
  createdAt: string;
  isAnswered: boolean;
  answerTitle: string | null;
  answerContent: string | null;
  answeredAt: string | null;
  answerReadAt: string | null;
};

export type InquiryFilter = {
  category?: InquiryCategory;
  isAnswered?: boolean;
};

/** 음식 아이콘. imageUrl은 앱과 같은 규칙(CDN 주소 + /icons/{iconId}.png)의 미리보기 주소다. */
export type AdminIcon = { iconId: number; name: string; imageUrl: string };

export type AdminIcons = { icons: AdminIcon[] };

/** 서버 검사와 같은 값. 화면은 먼저 안내만 하고, 기준은 서버다. */
export const ICON_NAME_MAX = 100;
export const ICON_IMAGE_MAX_BYTES = 512 * 1024;

export type GoogleClientResponse = { clientId: string };

/** 어드민 웹용 Google 클라이언트 ID. 공개 값이라 로그인 전에 받는다(빌드 시점에 박지 않고 백엔드 설정 하나만 쓴다). */
export const getGoogleClient = () => request<GoogleClientResponse>(`${PREFIX}/auth/google-client`);

export const login = (idToken: string) =>
  request<AdminResponse>(`${PREFIX}/auth/login`, { method: "POST", body: JSON.stringify({ idToken }) });

export const logout = () => request<void>(`${PREFIX}/auth/logout`, { method: "POST" });

export const getMe = () => request<AdminResponse>(`${PREFIX}/me`);

export function getInquiries(filter: InquiryFilter, cursor: number | null, size = 20) {
  const params = new URLSearchParams({ size: String(size) });
  if (filter.category) params.set("category", filter.category);
  if (filter.isAnswered !== undefined) params.set("isAnswered", String(filter.isAnswered));
  if (cursor !== null) params.set("cursor", String(cursor));

  return request<AdminInquiries>(`${PREFIX}/inquiries?${params}`);
}

export const getInquiry = (inquiryId: number) => request<AdminInquiry>(`${PREFIX}/inquiries/${inquiryId}`);

export const updateAnswer = (inquiryId: number, answerTitle: string, answerContent: string) =>
  request<AdminInquiry>(`${PREFIX}/inquiries/${inquiryId}/answer`, {
    method: "PUT",
    body: JSON.stringify({ answerTitle, answerContent }),
  });

export const getIcons = () => request<AdminIcons>(`${PREFIX}/icons`);

export function createIcon(name: string, image: File) {
  const body = new FormData();
  body.append("name", name);
  body.append("image", image);

  return request<AdminIcon>(`${PREFIX}/icons`, { method: "POST", body });
}

export const updateIconName = (iconId: number, name: string) =>
  request<AdminIcon>(`${PREFIX}/icons/${iconId}`, { method: "PATCH", body: JSON.stringify({ name }) });

export function updateIconImage(iconId: number, image: File) {
  const body = new FormData();
  body.append("image", image);

  return request<AdminIcon>(`${PREFIX}/icons/${iconId}/image`, { method: "PUT", body });
}
