import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createIcon,
  getIcons,
  ICON_IMAGE_MAX_BYTES,
  ICON_NAME_MAX,
  updateIconImage,
  updateIconName,
  type AdminIcon,
} from "../api/admin";
import { ImagePicker } from "../components/ImagePicker";

/**
 * 음식 아이콘 관리. 만들면 icons 행과 S3 icons/{iconId}.png가 함께 생기고, 앱은 iconId로 CDN 주소를 조립한다.
 */
export function IconListPage() {
  const icons = useQuery({ queryKey: ["icons"], queryFn: getIcons });
  const rows = icons.data?.icons ?? [];

  return (
    <section className="stack">
      <div className="toolbar">
        <h2>음식 아이콘</h2>
        {icons.isSuccess && <span className="muted">{rows.length}개</span>}
      </div>

      <p className="notice">새 음식 아이콘과 바꾼 이름은 최대 1시간 뒤부터 식사 분석(AI가 고르는 목록)에 쓰여요.</p>

      <CreateIconForm />

      {icons.isPending && <p className="muted">불러오는 중…</p>}
      {icons.error && <p className="error">{icons.error.message}</p>}
      {icons.isSuccess && rows.length === 0 && <p className="muted">음식 아이콘이 없습니다.</p>}

      {rows.length > 0 && (
        <div className="grid">
          {rows.map((icon) => (
            <IconCard key={icon.iconId} icon={icon} />
          ))}
        </div>
      )}
    </section>
  );
}

function CreateIconForm() {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [createdMessage, setCreatedMessage] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: (file: File) => createIcon(name.trim(), file),
    onSuccess: (created) => {
      setCreatedMessage(`#${created.iconId} "${created.name}" 음식 아이콘을 만들었어요.`);
      setName("");
      setImage(null);
      queryClient.invalidateQueries({ queryKey: ["icons"] });
    },
  });

  const canSubmit = name.trim().length > 0 && name.length <= ICON_NAME_MAX && image !== null && !create.isPending;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (canSubmit && image) create.mutate(image);
  };

  return (
    <form className="card form" onSubmit={handleSubmit}>
      <h3>음식 아이콘 만들기</h3>

      <label>
        이름
        <input
          type="text"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setCreatedMessage(null);
          }}
          maxLength={ICON_NAME_MAX}
        />
        <span className="counter">
          {name.length}/{ICON_NAME_MAX}
        </span>
      </label>

      <ImagePicker
        file={image}
        maxBytes={ICON_IMAGE_MAX_BYTES}
        onChange={(file) => {
          setImage(file);
          setCreatedMessage(null);
        }}
      />

      {create.error && <p className="error">{create.error.message}</p>}
      {createdMessage && <p className="success">{createdMessage}</p>}

      <button type="submit" className="primary" disabled={!canSubmit}>
        {create.isPending ? "만드는 중…" : "만들기"}
      </button>
    </form>
  );
}

type CardMode = "view" | "name" | "image";

function IconCard({ icon }: { icon: AdminIcon }) {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<CardMode>("view");
  const [name, setName] = useState(icon.name);
  const [image, setImage] = useState<File | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  // 교체 뒤에도 주소가 같아 브라우저가 옛 그림을 보여 주므로, 이 카드만 쿼리스트링을 바꿔 다시 받는다.
  const [imageVersion, setImageVersion] = useState<number | null>(null);

  const saveName = useMutation({
    mutationFn: () => updateIconName(icon.iconId, name.trim()),
    onSuccess: () => {
      setMode("view");
      setMessage("이름을 바꿨어요.");
      queryClient.invalidateQueries({ queryKey: ["icons"] });
    },
  });

  const replaceImage = useMutation({
    mutationFn: (file: File) => updateIconImage(icon.iconId, file),
    onSuccess: () => {
      setMode("view");
      setImage(null);
      setImageVersion(Date.now());
      setMessage("이미지를 바꿨어요. CDN 반영은 수 분, 앱 기기에는 최대 1시간 뒤 보여요.");
    },
  });

  const openMode = (next: CardMode) => {
    setMode(next);
    setName(icon.name);
    setImage(null);
    setMessage(null);
    saveName.reset();
    replaceImage.reset();
  };

  const imageSrc = imageVersion ? `${icon.imageUrl}?v=${imageVersion}` : icon.imageUrl;
  const canSaveName = name.trim().length > 0 && name.length <= ICON_NAME_MAX && !saveName.isPending;
  const error = saveName.error ?? replaceImage.error;

  return (
    <article className="thumb-card">
      <img src={imageSrc} alt={icon.name} className="thumb" />
      <div className="meta">
        <span>#{icon.iconId}</span>
      </div>
      {/* 운영자가 입력한 값도 텍스트로만 렌더링한다. */}
      <strong className="name">{icon.name}</strong>

      {mode === "view" && (
        <div className="actions">
          <button type="button" className="secondary" onClick={() => openMode("name")}>
            이름 수정
          </button>
          <button type="button" className="secondary" onClick={() => openMode("image")}>
            이미지 교체
          </button>
        </div>
      )}

      {mode === "name" && (
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault();
            if (canSaveName) saveName.mutate();
          }}
        >
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} maxLength={ICON_NAME_MAX} />
          <div className="actions">
            <button type="submit" className="secondary" disabled={!canSaveName}>
              {saveName.isPending ? "저장 중…" : "저장"}
            </button>
            <button type="button" className="link" onClick={() => openMode("view")}>
              취소
            </button>
          </div>
        </form>
      )}

      {mode === "image" && (
        <div className="form">
          <ImagePicker file={image} maxBytes={ICON_IMAGE_MAX_BYTES} onChange={setImage} />
          <div className="actions">
            <button
              type="button"
              className="secondary"
              disabled={image === null || replaceImage.isPending}
              onClick={() => image && replaceImage.mutate(image)}
            >
              {replaceImage.isPending ? "올리는 중…" : "교체"}
            </button>
            <button type="button" className="link" onClick={() => openMode("view")}>
              취소
            </button>
          </div>
        </div>
      )}

      {error && <p className="error">{error.message}</p>}
      {message && <p className="success">{message}</p>}
    </article>
  );
}
