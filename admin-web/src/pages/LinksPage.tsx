import { useState } from "react";
import { TOOL_GROUPS } from "../links";

/**
 * 어드민 첫 화면. 운영할 때 보는 외부 도구(GA4·Firebase·Metabase) 바로가기를 한곳에 모은다. 링크는 새 탭으로 연다.
 */
export function LinksPage() {
  return (
    <section className="stack">
      <div className="toolbar">
        <h2>바로가기</h2>
      </div>

      <p className="notice">GA4·Firebase는 Google 로그인과 프로젝트 권한이 있어야 열려요.</p>

      <div className="link-groups">
        {TOOL_GROUPS.map((group) => (
          <article key={group.title} className="card link-group">
            <div className="link-group-header">
              <h3>{group.title}</h3>
              <span className="badge">{group.tool}</span>
            </div>

            <ul className="link-list">
              {group.links.map((link) => (
                <li key={link.url}>
                  <a href={link.url} target="_blank" rel="noreferrer">
                    <strong>{link.label} ↗</strong>
                    <span className="muted">{link.description}</span>
                  </a>
                </li>
              ))}
            </ul>

            {group.command && <CommandBox description={group.command.description} value={group.command.value} />}
          </article>
        ))}
      </div>
    </section>
  );
}

type CopyState = "idle" | "copied" | "failed";

const COPY_LABELS: Record<CopyState, string> = {
  idle: "복사",
  copied: "복사했어요",
  failed: "직접 선택해 복사해 주세요",
};

function CommandBox({ description, value }: { description: string; value: string }) {
  const [copyState, setCopyState] = useState<CopyState>("idle");

  // 브라우저가 클립보드 접근을 막으면(권한·포커스) 실패를 알리고, 명령은 클릭으로 전체 선택할 수 있게 둔다.
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopyState("copied");
    } catch {
      setCopyState("failed");
    }
    setTimeout(() => setCopyState("idle"), 2000);
  };

  return (
    <div className="command">
      <p className="muted">{description}</p>
      <div className="command-row">
        <code>{value}</code>
        <button type="button" className="secondary" onClick={copy}>
          {COPY_LABELS[copyState]}
        </button>
      </div>
    </div>
  );
}
