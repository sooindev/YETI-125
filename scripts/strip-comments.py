#!/usr/bin/env python3
r"""
배포 산출물의 CSS/JS 주석을 걷어낸다 (소스는 건드리지 않는다).

    strip-comments.py <디렉터리>

mvn -Pprod 빌드가 target 안의 사본에 대고 부른다. 주석은 저장소에 그대로 남고
브라우저로 나가는 파일에서만 사라진다.

정규식으로 자르지 않는 이유 — 'https://...' 같은 문자열과 /^\d+$/ 같은 정규식
리터럴 안의 슬래시까지 주석으로 보고 잘라 코드를 깨뜨린다. 그래서 문자열·템플릿·
정규식 상태를 따라가는 스캐너로 훑는다.
"""
import os
import re
import sys

# 정규식 리터럴이 올 수 있는 자리인지 판단할 때 쓰는 키워드
KEYWORDS_BEFORE_REGEX = {
    'return', 'typeof', 'instanceof', 'in', 'of', 'new', 'delete', 'void',
    'case', 'do', 'else', 'yield', 'await', 'throw',
}
IDENT = re.compile(r'[A-Za-z0-9_$]')


def strip_js(src):
    out = []
    i, n = 0, len(src)

    def last_significant():
        """이미 내보낸 것 중 마지막 의미 있는 글자와, 그 앞의 낱말"""
        j = len(out) - 1
        while j >= 0 and out[j].isspace():
            j -= 1
        if j < 0:
            return '', ''
        ch = out[j]
        word = ''
        if IDENT.match(ch):
            k = j
            while k >= 0 and IDENT.match(out[k]):
                k -= 1
            word = ''.join(out[k + 1:j + 1])
        return ch, word

    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ''

        # ---- 주석 ----
        if c == '/' and nxt == '/':
            while i < n and src[i] != '\n':
                i += 1
            continue
        if c == '/' and nxt == '*':
            if src[i + 2:i + 3] == '!':        # /*! 라이선스 주석은 남긴다
                end = src.find('*/', i + 2)
                end = n if end < 0 else end + 2
                out.append(src[i:end])
                i = end
                continue
            end = src.find('*/', i + 2)
            end = n if end < 0 else end + 2
            # 줄을 걸친 주석은 줄바꿈 하나로 바꾼다 — 세미콜론 자동 삽입이 어긋나지 않게
            out.append('\n' if '\n' in src[i:end] else ' ')
            i = end
            continue

        # ---- 문자열 / 템플릿 ----
        if c in '"\'`':
            quote = c
            out.append(c)
            i += 1
            while i < n:
                d = src[i]
                out.append(d)
                if d == '\\':
                    if i + 1 < n:
                        out.append(src[i + 1])
                        i += 2
                        continue
                elif d == quote:
                    i += 1
                    break
                i += 1
            continue

        # ---- 정규식 리터럴인가, 나눗셈인가 ----
        if c == '/':
            ch, word = last_significant()
            divide = (ch in ')]}' or bool(IDENT.match(ch))) and word not in KEYWORDS_BEFORE_REGEX
            if divide:
                out.append(c)
                i += 1
                continue
            out.append(c)
            i += 1
            in_class = False
            while i < n:
                d = src[i]
                out.append(d)
                if d == '\\':
                    if i + 1 < n:
                        out.append(src[i + 1])
                        i += 2
                        continue
                elif d == '[':
                    in_class = True
                elif d == ']':
                    in_class = False
                elif d == '/' and not in_class:
                    i += 1
                    break
                elif d == '\n':                # 정규식은 줄을 넘지 않는다 — 판단이 틀린 것
                    i += 1
                    break
                i += 1
            continue

        out.append(c)
        i += 1

    return ''.join(out)


def strip_css(src):
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '/' and src[i + 1:i + 2] == '*':
            if src[i + 2:i + 3] == '!':
                end = src.find('*/', i + 2)
                end = n if end < 0 else end + 2
                out.append(src[i:end])
                i = end
                continue
            end = src.find('*/', i + 2)
            end = n if end < 0 else end + 2
            out.append('\n' if '\n' in src[i:end] else ' ')
            i = end
            continue
        if c in '"\'':
            quote = c
            out.append(c)
            i += 1
            while i < n:
                d = src[i]
                out.append(d)
                if d == '\\':
                    if i + 1 < n:
                        out.append(src[i + 1])
                        i += 2
                        continue
                elif d == quote:
                    i += 1
                    break
                i += 1
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def tidy(text):
    """주석이 빠진 자리에 남은 빈 줄과 줄 끝 공백을 정리한다"""
    text = '\n'.join(line.rstrip() for line in text.split('\n'))
    text = re.sub(r'\n{3,}', '\n\n', text)
    # 파일 첫머리 주석이 빠진 자리에 남는 빈 줄을 없앤다
    return text.lstrip('\n')


def main():
    if len(sys.argv) != 2:
        sys.exit('usage: strip-comments.py <디렉터리>')
    root = sys.argv[1]
    if not os.path.isdir(root):
        sys.exit(f'디렉터리가 아닙니다: {root}')

    saved = 0
    count = 0
    for dirpath, _, names in os.walk(root):
        for name in sorted(names):
            ext = os.path.splitext(name)[1].lower()
            if ext not in ('.css', '.js'):
                continue
            path = os.path.join(dirpath, name)
            src = open(path, encoding='utf-8').read()
            out = tidy(strip_js(src) if ext == '.js' else strip_css(src))
            if out != src:
                open(path, 'w', encoding='utf-8').write(out)
            saved += len(src.encode()) - len(out.encode())
            count += 1
            print(f'   {name:28} {len(src.encode()):>7} → {len(out.encode()):>7} B')
    print(f'   주석 제거: {count}개 파일, {saved:,} B 감소')


if __name__ == '__main__':
    main()
