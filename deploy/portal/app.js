(() => {
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  const revealItems = [...document.querySelectorAll("[data-reveal]")];
  const footer = document.querySelector(".site-footer");

  const show = (element) => element?.classList.add("is-in");

  if (reduceMotion.matches || !("IntersectionObserver" in window)) {
    revealItems.forEach(show);
    show(footer);
    return;
  }

  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        entry.target.classList.add("is-in");
        observer.unobserve(entry.target);
      }
    },
    { rootMargin: "0px 0px -12% 0px", threshold: 0.08 },
  );

  revealItems.forEach((item) => observer.observe(item));
  if (footer) observer.observe(footer);
})();
