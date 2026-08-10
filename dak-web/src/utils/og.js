/**
 * 기본 공유 이미지를 명시적으로 이름 붙여 둔 곳.
 *
 * app/opengraph-image.png는 파일 컨벤션으로 루트를 덮지만, Next의 metadata
 * 병합은 얕다. 자기 openGraph 객체를 반환하는 페이지는 부모가 해석해 둔
 * openGraph를 images까지 통째로 교체한다. 그런 페이지는 이미지를 다시
 * 지목해야 하고, 한 곳에 모아 두어야 페이지마다 값이 어긋나지 않는다.
 */
export const DEFAULT_OG_IMAGE = {
    url: '/opengraph-image.png',
    width: 1200,
    height: 630,
    alt: 'Discover Adelaide Korea',
  };