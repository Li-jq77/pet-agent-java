(function () {
    var nav = document.querySelector(".navbar");
    var toggle = document.getElementById("navToggle");
    var menu = document.getElementById("navMenu");
    if (!nav || !toggle || !menu) return;

    function setMenuOpen(open) {
        menu.classList.toggle("is-open", open);
        nav.classList.toggle("is-menu-open", open);
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        toggle.setAttribute("aria-label", open ? "关闭导航菜单" : "打开导航菜单");
    }

    function closeMenu() {
        setMenuOpen(false);
        Array.prototype.forEach.call(menu.querySelectorAll(".dropdown.is-open"), function (item) {
            item.classList.remove("is-open");
        });
    }

    toggle.addEventListener("click", function (event) {
        event.stopPropagation();
        setMenuOpen(!menu.classList.contains("is-open"));
    });

    menu.addEventListener("click", function (event) {
        var dropdownButton = event.target.closest(".dropbtn");
        if (dropdownButton && window.innerWidth <= 980) {
            event.preventDefault();
            var dropdown = dropdownButton.closest(".dropdown");
            var willOpen = !dropdown.classList.contains("is-open");
            Array.prototype.forEach.call(menu.querySelectorAll(".dropdown.is-open"), function (item) {
                item.classList.remove("is-open");
            });
            dropdown.classList.toggle("is-open", willOpen);
            return;
        }

        var link = event.target.closest("a[href]");
        if (link && window.innerWidth <= 980) closeMenu();
    });

    document.addEventListener("click", function (event) {
        if (!nav.contains(event.target)) closeMenu();
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") closeMenu();
    });

    window.addEventListener("resize", function () {
        if (window.innerWidth > 980) closeMenu();
    });
})();
