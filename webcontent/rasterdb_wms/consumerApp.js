function initConsumerApp() {

    Vue.component('consumer-app', {
	
        template: '#consumer-app-template',
        
        data: function () {
            return {
        
            visible: false,
            message: "cool",
        
            };
        },
        
        mounted: function() {
            var self = this;
            window.addEventListener('keyup', function (e) {
                if (e.keyCode == 27) {
                    console.log(e);
                    self.visible = false;
                }
            });
        },
        
        methods: {
            show() {
                this.visible = true;
            }
        },
        
    });

}