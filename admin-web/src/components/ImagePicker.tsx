import { useEffect, useRef, useState } from "react";

const PNG_TYPE = "image/png";

const formatBytes = (bytes: number) =>
  bytes >= 1024 * 1024 ? `${bytes / (1024 * 1024)}MB` : `${Math.round(bytes / 1024)}KB`;

/** 앱이 .png 주소로 이미지를 조립하므로 PNG만 받는다. 서버가 실제 내용으로 다시 검사한다. */
function validateImageFile(file: File, maxBytes: number): string | null {
  if (file.type !== PNG_TYPE) return "PNG 파일만 올릴 수 있어요.";
  if (file.size === 0) return "빈 파일이에요.";
  if (file.size > maxBytes) return `${formatBytes(maxBytes)} 이하만 올릴 수 있어요.`;
  return null;
}

type ImagePickerProps = {
  file: File | null;
  maxBytes: number;
  onChange: (file: File | null) => void;
};

/**
 * PNG 파일을 고르고 미리보기를 보여 준다. 형식·크기가 맞는 파일만 [onChange]로 넘기고, 아니면 안내 문구를 띄운다.
 * 부모가 file을 null로 바꾸면 입력칸도 비운다(업로드 성공 뒤 폼 초기화).
 */
export function ImagePicker({ file, maxBytes, onChange }: ImagePickerProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null);
      if (inputRef.current) inputRef.current.value = "";
      return;
    }

    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const handleChange = (selected: File | undefined) => {
    if (!selected) {
      setError(null);
      onChange(null);
      return;
    }

    const message = validateImageFile(selected, maxBytes);
    setError(message);
    onChange(message ? null : selected);
  };

  return (
    <div className="image-picker">
      <input
        ref={inputRef}
        type="file"
        accept={PNG_TYPE}
        onChange={(e) => handleChange(e.target.files?.[0])}
      />
      <span className="muted">PNG, {formatBytes(maxBytes)} 이하</span>
      {error && <p className="error">{error}</p>}
      {previewUrl && <img src={previewUrl} alt="올릴 이미지 미리보기" className="preview" />}
    </div>
  );
}
