
Ext.define('AMService', {
    extend: 'Ext.data.Model',
    fields: [
         {name: 'name', type: 'string'}
    ]
});

var svcStore = Ext.create('Ext.data.Store', {
	model: 'AMService',
	autoLoad: true,
	proxy: {
         type: 'ajax',
         url: '../restconsole/services/',
         reader: {
             type: 'json',
             root: 'serviceName'
             }
     }
});

var svcGrid = Ext.create('Ext.grid.Panel', {
    title: 'Services',
    region: 'west',
    width: '25%',
    margins: '0 5 5 5',
    bodyPadding: 5,
    store: svcStore,
    selType: 'rowmodel',
    listeners: {
        select: function( row, record, index,  eOpts ){ 
    		console.log('dblclick body'); 
    		console.log(record.get('name'));
    	}
    },
    columns: [
        { text: 'Service',  dataIndex: 'name', flex: 1}
    ]
});




var svcEditor = Ext.create('Ext.panel.Panel', {
    title: 'Editor',
    region: 'east',
    width: '75%',
    margins: '0 5 5 5',
	html: 'This is the text'
});

var globalPage = Ext.create('Ext.panel.Panel', {
	title: 'Global',
    layout:  'border',
	items: [
		svcGrid,
		svcEditor
	]
});


var tpanel = Ext.create('Ext.tab.Panel', {
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
});

Ext.application({
    name: 'Openam',
    launch: function() {
        Ext.create('Ext.container.Viewport', {
            layout: 'fit',
            items: [ 
            	tpanel
            ]
        })
    }
})

