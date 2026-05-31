(function () {
  "use strict";

  var slides = Array.prototype.slice.call(document.querySelectorAll(".campus-slide"));
  var dots = Array.prototype.slice.call(document.querySelectorAll(".gallery-dot"));
  var currentSlide = 0;
  var slideTimer;

  function showSlide(index) {
    currentSlide = index;
    slides.forEach(function (slide, slideIndex) {
      slide.classList.toggle("is-active", slideIndex === index);
    });
    dots.forEach(function (dot, dotIndex) {
      dot.classList.toggle("is-active", dotIndex === index);
    });
  }

  function scheduleSlides() {
    window.clearInterval(slideTimer);
    slideTimer = window.setInterval(function () {
      showSlide((currentSlide + 1) % slides.length);
    }, 6500);
  }

  dots.forEach(function (dot, index) {
    dot.addEventListener("click", function () {
      showSlide(index);
      scheduleSlides();
    });
  });

  function updateTime() {
    var now = new Date();
    document.getElementById("current-time").textContent = now.toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false
    });
  }

  function checkEndpoint(node) {
    fetch(node.getAttribute("data-endpoint"), { mode: "no-cors", cache: "no-store" })
      .then(function () {
        node.classList.remove("is-offline");
        node.lastChild.nodeValue = " 服务在线";
      })
      .catch(function () {
        node.classList.add("is-offline");
        node.lastChild.nodeValue = " 服务未启动";
      });
  }

  if (window.lucide) {
    window.lucide.createIcons();
  }

  document.querySelectorAll(".endpoint-state").forEach(checkEndpoint);
  window.setInterval(function () {
    document.querySelectorAll(".endpoint-state").forEach(checkEndpoint);
  }, 10000);

  updateTime();
  window.setInterval(updateTime, 1000);
  scheduleSlides();
})();
