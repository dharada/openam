


var globalPage = Ext.create('Ext.panel.Panel', {
	title: 'Global',
    layout:  'border',
	items: [ {
        xtype: 'servicelist',
        region: 'west',
        width: '25%',
        store: svcStore
        },{
        xtype: 'serviceEditor',
        region: 'west',
        width: '25%',
        html: 'This is the editor text'
        }
	]
});

Ext.application({
    name: 'Openam',
    appFolder: 'console',

    controllers: [
         'Global'
    ],
    launch: function() {
        Ext.create('Ext.container.Viewport', {
            layout: 'fit',
            items: [
                {
                    xtype: tabpanel,
                    activeTab: 0,
                    bodyPadding: 10,
                    tabPosition: 'top',
                    layout: 'fit',
                    items: [
                        globalPage,
                        {
                            // xtype: 'panel' implied by default
                            title: 'Realms',
                            region:'west',
                            xtype: 'panel',
                            margins: '5 0 0 5',
                            collapsible: true,   // make collapsible
                            id: 'west-region-container',
                            layout: 'fit'
                        }
                    ]
                }
            ]
        })
    }
})

