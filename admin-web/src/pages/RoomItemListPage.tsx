import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import {
  createRoomItem,
  getRoomItems,
  ROOM_ITEM_CATEGORY_LABELS,
  ROOM_ITEM_IMAGE_MAX_BYTES,
  ROOM_ITEM_NAME_MAX,
  updateRoomItem,
  updateRoomItemImage,
  type AdminRoomItem,
  type RoomItemCategory,
} from "../api/admin";
import { ImagePicker } from "../components/ImagePicker";

const CATEGORIES = Object.keys(ROOM_ITEM_CATEGORY_LABELS) as RoomItemCategory[];

/** 빈칸은 비매품(null), 그 외에는 0 이상의 정수만 받는다. 형식이 틀리면 undefined. */
const convertPrice = (value: string): number | null | undefined => {
  const trimmed = value.trim();
  if (trimmed === "") return null;
  return /^\d+$/.test(trimmed) ? Number(trimmed) : undefined;
};

const formatPrice = (price: number | null) => (price === null ? "비매품" : `${price.toLocaleString()} 코인`);

/**
 * 마이룸 아이템 관리. 만들면 items·room_items 행과 S3 items/{itemId}.png가 함께 생기고, 앱은 itemId로 CDN 주소를 조립한다.
 * 카테고리 필터는 주소(?category=)에 둬서 새로고침·뒤로 가기에도 유지된다.
 */
export function RoomItemListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const category = (searchParams.get("category") as RoomItemCategory | null) ?? undefined;

  const roomItems = useQuery({
    queryKey: ["roomItems", category ?? "all"],
    queryFn: () => getRoomItems(category),
  });
  const rows = roomItems.data?.roomItems ?? [];

  const updateCategory = (value: RoomItemCategory | undefined) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set("category", value);
    else next.delete("category");
    setSearchParams(next, { replace: true });
  };

  return (
    <section className="stack">
      <div className="toolbar">
        <h2>마이룸 아이템</h2>
        <div className="segmented">
          <button type="button" className={category === undefined ? "active" : ""} onClick={() => updateCategory(undefined)}>
            전체
          </button>
          {CATEGORIES.map((value) => (
            <button
              key={value}
              type="button"
              className={value === category ? "active" : ""}
              onClick={() => updateCategory(value)}
            >
              {ROOM_ITEM_CATEGORY_LABELS[value]}
            </button>
          ))}
        </div>
      </div>

      <p className="notice">
        만들어도 사용자에게 바로 보이지는 않아요. 기본 지급 설정이나 상점·보상에 넣어야 사용자에게 가요. 이름은 사용자의 보유 아이템
        목록·우편 보상에 바로 반영돼요.
      </p>

      <CreateRoomItemForm defaultCategory={category ?? "BACKGROUND"} />

      {roomItems.isPending && <p className="muted">불러오는 중…</p>}
      {roomItems.error && <p className="error">{roomItems.error.message}</p>}
      {roomItems.isSuccess && rows.length === 0 && <p className="muted">마이룸 아이템이 없습니다.</p>}

      {rows.length > 0 && (
        <div className="grid">
          {rows.map((roomItem) => (
            <RoomItemCard key={roomItem.itemId} roomItem={roomItem} />
          ))}
        </div>
      )}
    </section>
  );
}

function CreateRoomItemForm({ defaultCategory }: { defaultCategory: RoomItemCategory }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [priceText, setPriceText] = useState("");
  const [category, setCategory] = useState<RoomItemCategory>(defaultCategory);
  const [image, setImage] = useState<File | null>(null);
  const [createdMessage, setCreatedMessage] = useState<string | null>(null);

  const price = convertPrice(priceText);

  const create = useMutation({
    mutationFn: (file: File) => createRoomItem({ name: name.trim(), price: price ?? null, category, image: file }),
    onSuccess: (created) => {
      setCreatedMessage(`#${created.itemId} "${created.name}" 마이룸 아이템을 만들었어요.`);
      setName("");
      setPriceText("");
      setImage(null);
      queryClient.invalidateQueries({ queryKey: ["roomItems"] });
    },
  });

  const canSubmit =
    name.trim().length > 0 &&
    name.length <= ROOM_ITEM_NAME_MAX &&
    price !== undefined &&
    image !== null &&
    !create.isPending;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (canSubmit && image) create.mutate(image);
  };

  const clearMessage = () => setCreatedMessage(null);

  return (
    <form className="card form" onSubmit={handleSubmit}>
      <h3>마이룸 아이템 만들기</h3>

      <label>
        이름
        <input
          type="text"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            clearMessage();
          }}
          maxLength={ROOM_ITEM_NAME_MAX}
        />
        <span className="counter">
          {name.length}/{ROOM_ITEM_NAME_MAX}
        </span>
      </label>

      <div className="form-row">
        <label>
          가격(코인)
          <input
            type="text"
            inputMode="numeric"
            placeholder="비우면 비매품"
            value={priceText}
            onChange={(e) => {
              setPriceText(e.target.value);
              clearMessage();
            }}
          />
        </label>
        <label>
          카테고리
          <select
            value={category}
            onChange={(e) => {
              setCategory(e.target.value as RoomItemCategory);
              clearMessage();
            }}
          >
            {CATEGORIES.map((value) => (
              <option key={value} value={value}>
                {ROOM_ITEM_CATEGORY_LABELS[value]}
              </option>
            ))}
          </select>
        </label>
      </div>
      {price === undefined && <p className="error">가격은 0 이상의 정수로 적거나, 비매품이면 비워 주세요.</p>}

      <ImagePicker
        file={image}
        maxBytes={ROOM_ITEM_IMAGE_MAX_BYTES}
        onChange={(file) => {
          setImage(file);
          clearMessage();
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

type CardMode = "view" | "edit" | "image";

function RoomItemCard({ roomItem }: { roomItem: AdminRoomItem }) {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<CardMode>("view");
  const [name, setName] = useState(roomItem.name);
  const [priceText, setPriceText] = useState(roomItem.price === null ? "" : String(roomItem.price));
  const [image, setImage] = useState<File | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  // 교체 뒤에도 주소가 같아 브라우저가 옛 그림을 보여 주므로, 이 카드만 쿼리스트링을 바꿔 다시 받는다.
  const [imageVersion, setImageVersion] = useState<number | null>(null);

  const price = convertPrice(priceText);

  const save = useMutation({
    mutationFn: () => updateRoomItem(roomItem.itemId, name.trim(), price ?? null),
    onSuccess: () => {
      setMode("view");
      setMessage("이름·가격을 바꿨어요.");
      queryClient.invalidateQueries({ queryKey: ["roomItems"] });
    },
  });

  const replaceImage = useMutation({
    mutationFn: (file: File) => updateRoomItemImage(roomItem.itemId, file),
    onSuccess: () => {
      setMode("view");
      setImage(null);
      setImageVersion(Date.now());
      setMessage("이미지를 바꿨어요. CDN 반영은 수 분, 앱 기기에는 최대 1시간 뒤 보여요.");
    },
  });

  const openMode = (next: CardMode) => {
    setMode(next);
    setName(roomItem.name);
    setPriceText(roomItem.price === null ? "" : String(roomItem.price));
    setImage(null);
    setMessage(null);
    save.reset();
    replaceImage.reset();
  };

  const imageSrc = imageVersion ? `${roomItem.imageUrl}?v=${imageVersion}` : roomItem.imageUrl;
  const canSave = name.trim().length > 0 && name.length <= ROOM_ITEM_NAME_MAX && price !== undefined && !save.isPending;
  const error = save.error ?? replaceImage.error;

  return (
    <article className="thumb-card">
      <img src={imageSrc} alt={roomItem.name} className="thumb" />
      <div className="meta">
        <span>#{roomItem.itemId}</span>
        <span className="badge">{ROOM_ITEM_CATEGORY_LABELS[roomItem.category]}</span>
        <span>{formatPrice(roomItem.price)}</span>
      </div>
      {/* 운영자가 입력한 값도 텍스트로만 렌더링한다. */}
      <strong className="name">{roomItem.name}</strong>

      {mode === "view" && (
        <div className="actions">
          <button type="button" className="secondary" onClick={() => openMode("edit")}>
            이름·가격 수정
          </button>
          <button type="button" className="secondary" onClick={() => openMode("image")}>
            이미지 교체
          </button>
        </div>
      )}

      {mode === "edit" && (
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault();
            if (canSave) save.mutate();
          }}
        >
          <input
            type="text"
            aria-label="이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={ROOM_ITEM_NAME_MAX}
          />
          <input
            type="text"
            aria-label="가격"
            inputMode="numeric"
            placeholder="비우면 비매품"
            value={priceText}
            onChange={(e) => setPriceText(e.target.value)}
          />
          {price === undefined && <p className="error">가격은 0 이상의 정수로 적거나 비워 주세요.</p>}
          <div className="actions">
            <button type="submit" className="secondary" disabled={!canSave}>
              {save.isPending ? "저장 중…" : "저장"}
            </button>
            <button type="button" className="link" onClick={() => openMode("view")}>
              취소
            </button>
          </div>
        </form>
      )}

      {mode === "image" && (
        <div className="form">
          <p className="notice">기본 지급 아이템을 바꾸면 그 아이템을 가진 모든 사용자의 방이 바뀌어요.</p>
          <ImagePicker file={image} maxBytes={ROOM_ITEM_IMAGE_MAX_BYTES} onChange={setImage} />
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
