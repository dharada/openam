


Ext.application({
    name: 'AM',
    appFolder: 'console',

    requires: [
        'Ext.container.Viewport'
    ] ,

    controllers: [
         'Global'
    ],
    launch: function() {
        Ext.create('Ext.container.Viewport', {
            layout: 'fit',
            items: [
                {
                    xtype: 'globaleditor',
                    title: 'Global',
                    width: '100%'
                }
            ]
        });
    }
});

